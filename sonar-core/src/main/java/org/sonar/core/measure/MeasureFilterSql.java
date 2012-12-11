/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.core.measure;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.Metric;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.DatabaseUtils;
import org.sonar.core.persistence.dialect.PostgreSql;
import org.sonar.core.resource.SnapshotDto;

import javax.annotation.Nullable;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class MeasureFilterSql {

  private final Database database;
  private final MeasureFilter filter;
  private final MeasureFilterContext context;
  private final StringBuilder sql = new StringBuilder(1000);
  private final List<Date> dateParameters = Lists.newArrayList();

  MeasureFilterSql(Database database, MeasureFilter filter, MeasureFilterContext context) {
    this.database = database;
    this.filter = filter;
    this.context = context;
    init();
  }

  List<MeasureFilterRow> execute(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement(sql.toString());
    ResultSet rs = null;
    try {
      for (int index = 0; index < dateParameters.size(); index++) {
        statement.setDate(index + 1, dateParameters.get(index));
      }
      rs = statement.executeQuery();
      return process(rs);

    } finally {
      DatabaseUtils.closeQuietly(rs);
      DatabaseUtils.closeQuietly(statement);
    }
  }

  String sql() {
    return sql.toString();
  }

  private void init() {
    sql.append("SELECT block.id, max(block.rid) AS rid, max(block.rootid) AS rootid, max(sortval) AS sortval1, CASE WHEN sortval IS NULL THEN 1 ELSE 0 END AS sortval2 ");
    for (int index = 0; index < filter.getMeasureConditions().size(); index++) {
      sql.append(", max(crit_").append(index).append(")");
    }
    sql.append(" FROM (");

    appendSortBlock();
    for (int index = 0; index < filter.getMeasureConditions().size(); index++) {
      MeasureFilterCondition condition = filter.getMeasureConditions().get(index);
      sql.append(" UNION ");
      appendConditionBlock(index, condition);
    }

    sql.append(") block GROUP BY block.id, sortval2");
    if (!filter.getMeasureConditions().isEmpty()) {
      sql.append(" HAVING ");
      for (int index = 0; index < filter.getMeasureConditions().size(); index++) {
        if (index > 0) {
          sql.append(" AND ");
        }
        sql.append(" max(crit_").append(index).append(") IS NOT NULL ");
      }
    }
    if (filter.sort().isSortedByDatabase()) {
      sql.append(" ORDER BY sortval2 ASC, sortval1 ");
      sql.append(filter.sort().isAsc() ? "ASC " : "DESC ");
    }
  }

  private void appendSortBlock() {
    sql.append(" SELECT s.id, s.project_id AS rid, s.root_project_id AS rootid, ").append(filter.sort().column()).append(" AS sortval ");
    for (int index = 0; index < filter.getMeasureConditions().size(); index++) {
      MeasureFilterCondition condition = filter.getMeasureConditions().get(index);
      sql.append(", ").append(nullSelect(condition.metric())).append(" AS crit_").append(index);
    }
    sql.append(" FROM snapshots s INNER JOIN projects p ON s.project_id=p.id ");
    if (filter.isOnFavourites()) {
      sql.append(" INNER JOIN properties props ON props.resource_id=s.project_id ");
    }
    if (filter.sort().onMeasures()) {
      sql.append(" LEFT OUTER JOIN project_measures pm ON s.id=pm.snapshot_id AND pm.metric_id=");
      sql.append(filter.sort().metric().getId());
      sql.append(" AND pm.rule_id IS NULL AND pm.rule_priority IS NULL AND pm.characteristic_id IS NULL AND pm.person_id IS NULL ");
    }
    sql.append(" WHERE ");
    appendResourceConditions();
  }

  private void appendConditionBlock(int conditionIndex, MeasureFilterCondition condition) {
    sql.append(" SELECT s.id, s.project_id AS rid, s.root_project_id AS rootid, null AS sortval ");
    for (int j = 0; j < filter.getMeasureConditions().size(); j++) {
      sql.append(", ");
      if (j == conditionIndex) {
        sql.append(condition.valueColumn());
      } else {
        sql.append(nullSelect(filter.getMeasureConditions().get(j).metric()));
      }
      sql.append(" AS crit_").append(j);
    }
    sql.append(" FROM snapshots s INNER JOIN projects p ON s.project_id=p.id INNER JOIN project_measures pm ON s.id=pm.snapshot_id ");
    if (filter.isOnFavourites()) {
      sql.append(" INNER JOIN properties props ON props.resource_id=s.project_id ");
    }
    sql.append(" WHERE ");
    appendResourceConditions();
    sql.append(" AND pm.rule_id IS NULL AND pm.rule_priority IS NULL AND pm.characteristic_id IS NULL AND pm.person_id IS NULL AND ");
    condition.appendSqlCondition(sql);
  }

  private void appendResourceConditions() {
    sql.append(" s.status='P' AND s.islast=").append(database.getDialect().getTrueSqlValue());
    if (context.getBaseSnapshot() == null) {
      sql.append(" AND p.copy_resource_id IS NULL ");
    }
    if (!filter.getResourceQualifiers().isEmpty()) {
      sql.append(" AND s.qualifier IN ");
      appendInStatement(filter.getResourceQualifiers(), sql);
    }
    if (!filter.getResourceScopes().isEmpty()) {
      sql.append(" AND s.scope IN ");
      appendInStatement(filter.getResourceScopes(), sql);
    }
    if (!filter.getResourceLanguages().isEmpty()) {
      sql.append(" AND p.language IN ");
      appendInStatement(filter.getResourceLanguages(), sql);
    }
    appendDateConditions();
    appendFavouritesCondition();
    appendResourceNameCondition();
    appendResourceKeyCondition();
    appendResourceBaseCondition();
  }

  private void appendDateConditions() {
    if (filter.getFromDate() != null) {
      sql.append(" AND s.created_at >= ? ");
      dateParameters.add(new Date(filter.getFromDate().getTime()));
    }
    if (filter.getToDate() != null) {
      sql.append(" AND s.created_at <= ? ");
      dateParameters.add(new Date(filter.getToDate().getTime()));
    }
  }

  private void appendFavouritesCondition() {
    if (filter.isOnFavourites()) {
      sql.append(" AND props.prop_key='favourite' AND props.resource_id IS NOT NULL AND props.user_id=");
      sql.append(context.getUserId());
      sql.append(" ");
    }
  }

  private void appendResourceBaseCondition() {
    SnapshotDto baseSnapshot = context.getBaseSnapshot();
    if (baseSnapshot != null) {
      if (filter.isOnBaseResourceChildren()) {
        sql.append(" AND s.parent_snapshot_id=").append(baseSnapshot.getId());
      } else {
        Long rootSnapshotId = (baseSnapshot.getRootId() != null ? baseSnapshot.getRootId() : baseSnapshot.getId());
        sql.append(" AND s.root_snapshot_id=").append(rootSnapshotId);
        sql.append(" AND s.path LIKE '").append(StringUtils.defaultString(baseSnapshot.getPath())).append(baseSnapshot.getId()).append(".%'");
      }
    }
  }

  private void appendResourceKeyCondition() {
    if (StringUtils.isNotBlank(filter.getResourceKeyRegexp())) {
      sql.append(" AND UPPER(p.kee) LIKE '");
      // limitation : special characters _ and % are not escaped
      String regexp = StringEscapeUtils.escapeSql(filter.getResourceKeyRegexp());
      regexp = StringUtils.replaceChars(regexp, '*', '%');
      regexp = StringUtils.replaceChars(regexp, '?', '_');
      sql.append(StringUtils.upperCase(regexp)).append("'");
    }
  }

  private void appendResourceNameCondition() {
    if (StringUtils.isNotBlank(filter.getResourceName())) {
      sql.append(" AND s.project_id IN (SELECT rindex.resource_id FROM resource_index rindex WHERE rindex.kee like '");
      sql.append(StringEscapeUtils.escapeSql(StringUtils.lowerCase(filter.getResourceName())));
      sql.append("%'");
      if (!filter.getResourceQualifiers().isEmpty()) {
        sql.append(" AND rindex.qualifier IN ");
        appendInStatement(filter.getResourceQualifiers(), sql);
      }
      sql.append(") ");
    }
  }

  List<MeasureFilterRow> process(ResultSet rs) throws SQLException {
    List<MeasureFilterRow> rows = Lists.newArrayList();
    boolean sortTextValues = !filter.sort().isSortedByDatabase();
    while (rs.next()) {
      MeasureFilterRow row = new MeasureFilterRow(rs.getLong(1), rs.getLong(2), rs.getLong(3));
      if (sortTextValues) {
        row.setSortText(rs.getString(4));
      }
      rows.add(row);
    }
    if (sortTextValues) {
      // database does not manage case-insensitive text sorting. It must be done programmatically
      Function<MeasureFilterRow, String> function = new Function<MeasureFilterRow, String>() {
        public String apply(@Nullable MeasureFilterRow row) {
          return (row != null ? StringUtils.defaultString(row.getSortText()) : "");
        }
      };
      Ordering<MeasureFilterRow> ordering = Ordering.from(String.CASE_INSENSITIVE_ORDER).onResultOf(function).nullsFirst();
      if (!filter.sort().isAsc()) {
        ordering = ordering.reverse();
      }
      rows = ordering.sortedCopy(rows);
    }
    return rows;
  }

  private String nullSelect(Metric metric) {
    if (metric.isNumericType() && PostgreSql.ID.equals(database.getDialect().getId())) {
      return "null::integer";
    }
    return "null";
  }


  private static void appendInStatement(List<String> values, StringBuilder to) {
    to.append(" ('");
    to.append(StringUtils.join(values, "','"));
    to.append("') ");
  }
}
