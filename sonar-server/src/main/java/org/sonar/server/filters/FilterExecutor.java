/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.filters;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;

import javax.persistence.Query;

public class FilterExecutor implements ServerComponent {
  private static final Logger LOG = LoggerFactory.getLogger(FilterExecutor.class);
  private static final int SQL_INITIAL_SIZE = 1000;
  private DatabaseSession session;

  public FilterExecutor(DatabaseSession session) {
    this.session = session;
  }

  public FilterResult execute(Filter filter) {
    String sql = null;
    try {
      TimeProfiler profiler = new TimeProfiler(FilterExecutor.class).start("Build/execute SQL query");
      sql = toSql(filter);
      LOG.info("SQL: " + sql);
      Query query = session.getEntityManager().createNativeQuery(sql);
      setHqlParameters(filter, query);
      FilterResult result = new FilterResult(filter, query.getResultList());
      profiler.stop();

      profiler.start("Process rows");
      result.removeUnvalidRows();
      profiler.stop();

      profiler.start("Sort rows");
      result.sort();
      profiler.stop();
      return result;

    } catch (Exception e) {
      throw new SonarException("Fail to execute filter: " + filter.toString() + ", sql=" + sql, e);
    }
  }

  private String toSql(Filter filter) {
    StringBuilder sql = new StringBuilder(SQL_INITIAL_SIZE);
    addSelectColumns(filter, sql);
    addFromClause(filter, sql);
    addWhereClause(filter, sql);
    return sql.toString();
  }

  private void addSelectColumns(Filter filter, StringBuilder sql) {
    sql.append("SELECT s.id, MAX(s.project_id) as pid, MAX(s.root_project_id) as rpid");
    if (filter.isSortedByLanguage()) {
      sql.append(", MAX(p.language) as lang ");

    } else if (filter.isSortedByName()) {
      sql.append(", MAX(p.long_name) as name ");

    } else if (filter.isSortedByDate()) {
      sql.append(", MAX(s.created_at) as createdat ");

    } else if (filter.isSortedByVersion()) {
      sql.append(", MAX(s.version) as version ");
    }
    if (filter.getSortedMetricId() != null) {
      sql.append(", MAX(CASE WHEN pm.metric_id=");
      sql.append(filter.getSortedMetricId());
      sql.append(" THEN ");
      sql.append(filter.useMeasureValueToSort() ? "value" : "text_value");
      sql.append(" ELSE NULL END) AS sortvalue");
      sql.append(" ");
    }
    for (int index = 0; index < filter.getMeasureCriteria().size(); index++) {
      MeasureCriterion criterion = filter.getMeasureCriteria().get(index);
      sql.append(", MAX(CASE WHEN pm.metric_id=");
      sql.append(criterion.getMetricId());
      sql.append(" AND pm.value");
      sql.append(criterion.getOperator());
      sql.append(criterion.getValue());
      sql.append(" THEN value ELSE NULL END) AS crit_");
      sql.append(index);
      sql.append(" ");
    }
  }

  private void addFromClause(Filter filter, StringBuilder sql) {
    sql.append(" FROM snapshots s ");
    if (filter.mustJoinMeasuresTable()) {
      sql.append(" INNER JOIN project_measures pm ON s.id=pm.snapshot_id ");
    }
    sql.append(" INNER JOIN projects p ON s.project_id=p.id ");
  }

  private void addWhereClause(Filter filter, StringBuilder sql) {
    sql.append(" WHERE ");
    if (filter.mustJoinMeasuresTable()) {
      if (filter.hasMeasureCriteria()) {
        sql.append(" ( ");
        int index = 0;
        while (index < filter.getMeasureCriteria().size()) {
          if (index > 0) {
            sql.append(" OR ");
          }
          MeasureCriterion criteria = filter.getMeasureCriteria().get(index);
          sql.append("(pm.metric_id=").append(criteria.getMetricId()).append(" and pm.value")
              .append(criteria.getOperator()).append(criteria.getValue()).append(")");
          index++;
        }

        if (filter.getSortedMetricId() != null && !filter.hasMeasureCriteriaOnMetric(filter.getSortedMetricId())) {
          sql.append(" OR (pm.metric_id=").append(filter.getSortedMetricId()).append(") ");
        }

        sql.append(" ) AND ");
      }
      sql.append(" pm.rule_id is null AND pm.rules_category_id is null AND pm.rule_priority is null AND pm.characteristic_id IS NULL AND ");
    }
    sql.append(" s.status=:status AND s.islast=:islast ");
    if (filter.getScopes() != null) {
      sql.append(filter.getScopes().isEmpty() ? " AND s.scope IS NULL " : " AND s.scope IN (:scopes) ");
    }
    if (filter.hasQualifiers()) {
      sql.append(" AND s.qualifier IN (:qualifiers) ");
    } else {
      sql.append(" AND s.qualifier IS NULL ");
    }
    if (filter.hasLanguages()) {
      sql.append(" AND p.language IN (:languages) ");
    }
    if (filter.getFavouriteIds() != null) {
      sql.append(filter.getFavouriteIds().isEmpty() ? " AND s.project_id IS NULL " : " AND s.project_id IN (:favourites) ");
    }
    if (filter.hasBaseSnapshot()) {
      sql.append(" AND s.root_snapshot_id=:root_sid AND s.path LIKE :path ");
    }
    if (filter.getDateCriterion() != null) {
      sql.append(" AND s.created_at");
      sql.append(filter.getDateCriterion().getOperator());
      sql.append(" :date ");
    }
    if (StringUtils.isNotBlank(filter.getKeyRegexp())) {
      sql.append(" AND UPPER(p.kee) LIKE :kee");
    }
    if (StringUtils.isNotBlank(filter.getNameRegexp())) {
      sql.append(" AND UPPER(p.long_name) LIKE :name");
    }
    if (!filter.isViewContext()) {
      sql.append(" AND p.copy_resource_id IS NULL ");
    }
    sql.append(" GROUP BY s.id");
  }

  private void setHqlParameters(Filter filter, Query query) {
    query.setParameter("status", Snapshot.STATUS_PROCESSED);
    query.setParameter("islast", true);
    if (filter.hasScopes()) {
      query.setParameter("scopes", filter.getScopes());
    }
    if (filter.hasQualifiers()) {
      query.setParameter("qualifiers", filter.getQualifiers());
    }
    if (filter.hasLanguages()) {
      query.setParameter("languages", filter.getLanguages());
    }
    if (filter.hasFavouriteIds()) {
      query.setParameter("favourites", filter.getFavouriteIds());
    }
    if (filter.getDateCriterion() != null) {
      query.setParameter("date", filter.getDateCriterion().getDate());
    }
    if (filter.hasBaseSnapshot()) {
      query.setParameter("root_sid", filter.getRootSnapshotId());
      query.setParameter("path", new StringBuilder().append(
          filter.getBaseSnapshotPath()).append(filter.getBaseSnapshotId()).append(".%").toString());
    }
    if (StringUtils.isNotBlank(filter.getKeyRegexp())) {
      query.setParameter("kee", StringUtils.upperCase(StringUtils.replaceChars(filter.getKeyRegexp(), '*', '%')));
    }
    if (StringUtils.isNotBlank(filter.getNameRegexp())) {
      query.setParameter("name", StringUtils.upperCase(StringUtils.replaceChars(filter.getNameRegexp(), '*', '%')));
    }
  }
}
