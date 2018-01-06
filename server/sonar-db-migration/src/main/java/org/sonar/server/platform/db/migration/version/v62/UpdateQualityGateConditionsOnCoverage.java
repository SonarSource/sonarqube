/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.db.migration.version.v62;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.Upsert;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

/**
 * Migrate every quality gates that have conditions on related coverage metrics.
 *
 * If there's a condition on {@link CoreMetrics#OVERALL_COVERAGE}, it will be updated to {@link CoreMetrics#COVERAGE},
 * conditions on {@link CoreMetrics#IT_COVERAGE} are removed.
 * Else if there's condition on {@link CoreMetrics#COVERAGE}, it will be kept and conditions on {@link CoreMetrics#IT_COVERAGE} are removed.
 * Then If there's condition on {@link CoreMetrics#IT_COVERAGE}, it will be updated to {@link CoreMetrics#COVERAGE}
 *
 * Same strategy is applied on new_XXX, (it_|overall_)lines_to_cover, (it_|overall_)uncovered_lines, etc. related coverage metrics.
 */
public class UpdateQualityGateConditionsOnCoverage extends DataChange {

  private static final Logger LOGGER = Loggers.get(UpdateQualityGateConditionsOnCoverage.class);

  private static final List<String> COVERAGE_METRIC_KEYS = ImmutableList.of(
    "coverage", "lines_to_cover", "uncovered_lines", "line_coverage", "conditions_to_cover", "uncovered_conditions", "branch_coverage");

  private static final String OVERALL_PREFIX = "overall_";
  private static final String IT_PREFIX = "it_";
  private static final String NEW_PREFIX = "new_";

  public UpdateQualityGateConditionsOnCoverage(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    List<Metric> metrics = selectMetrics(context);
    if (metrics.isEmpty()) {
      return;
    }
    List<Long> qualityGateIds = context.prepareSelect("select id from quality_gates").list(Select.LONG_READER);
    if (qualityGateIds.isEmpty()) {
      return;
    }
    LOGGER.info("Migrating {} quality gates", qualityGateIds.size());
    new Migration(context, metrics, qualityGateIds).execute();
  }

  private static class Migration {
    private final Context context;
    private final Map<String, Metric> metricsByMetricKeys;
    private final List<Long> metricIds;
    private final List<Long> qualityGateIds;

    Migration(Context context, List<Metric> metrics, List<Long> qualityGateIds) {
      this.context = context;
      this.metricsByMetricKeys = metrics.stream().collect(uniqueIndex(Metric::getKey, Function.identity()));
      this.metricIds = metrics.stream().map(Metric::getId).collect(Collectors.toList());
      this.qualityGateIds = qualityGateIds;
    }

    public void execute() {
      qualityGateIds.forEach(this::processQualityGate);
    }

    private void processQualityGate(long qualityGateId) {
      List<QualityGateCondition> qualityGateConditions = selectQualityGateConditions(qualityGateId, metricIds);
      Multimap<Long, QualityGateCondition> qualityGateConditionsByMetricId = qualityGateConditions.stream()
        .collect(index(QualityGateCondition::getMetricId, Function.identity()));
      COVERAGE_METRIC_KEYS.forEach(metric -> {
        processConditions(metric, OVERALL_PREFIX + metric, IT_PREFIX + metric, qualityGateConditionsByMetricId, qualityGateId);
        processConditions(NEW_PREFIX + metric, NEW_PREFIX + OVERALL_PREFIX + metric, NEW_PREFIX + IT_PREFIX + metric, qualityGateConditionsByMetricId, qualityGateId);
      });
    }

    private void processConditions(String coverageMetricKey, String overallMetricKey, String itMetricKey, Multimap<Long, QualityGateCondition> qualityGateConditionsByMetricId,
      long qualityGateId) {
      try {
        Collection<QualityGateCondition> conditionsOnCoverage = getConditionsByMetricKey(coverageMetricKey, qualityGateConditionsByMetricId);
        Collection<QualityGateCondition> conditionsOnOverallCoverage = getConditionsByMetricKey(overallMetricKey, qualityGateConditionsByMetricId);
        Collection<QualityGateCondition> conditionsOnItCoverage = getConditionsByMetricKey(itMetricKey, qualityGateConditionsByMetricId);
        if (conditionsOnCoverage.isEmpty() && conditionsOnOverallCoverage.isEmpty() && conditionsOnItCoverage.isEmpty()) {
          return;
        }
        if (!conditionsOnOverallCoverage.isEmpty()) {
          removeQualityGateConditions(conditionsOnCoverage);
          removeQualityGateConditions(conditionsOnItCoverage);
          updateQualityGateConditions(conditionsOnOverallCoverage, coverageMetricKey);
        } else if (!conditionsOnCoverage.isEmpty()) {
          removeQualityGateConditions(conditionsOnItCoverage);
        } else {
          updateQualityGateConditions(conditionsOnItCoverage, coverageMetricKey);
        }
      } catch (SQLException e) {
        throw new IllegalStateException(String.format("Fail to update quality gate conditions of quality gate %s", qualityGateId), e);
      }
    }

    private Collection<QualityGateCondition> getConditionsByMetricKey(String metricKey, Multimap<Long, QualityGateCondition> qualityGateConditionsByMetricId) {
      Metric metric = metricsByMetricKeys.get(metricKey);
      if (metric == null) {
        return Collections.emptyList();
      }
      return qualityGateConditionsByMetricId.get(metric.getId());
    }

    private List<QualityGateCondition> selectQualityGateConditions(long qualityGateId, List<Long> metricIds) {
      try {
        Select select = context.prepareSelect("select qgc.id, qgc.metric_id " +
          "from quality_gate_conditions qgc " +
          "where qgc.qgate_id=? and qgc.metric_id in (" + repeat("?", " , ", metricIds.size()) + ")")
          .setLong(1, qualityGateId);
        for (int i = 0; i < metricIds.size(); i++) {
          select.setLong(i + 2, metricIds.get(i));
        }
        return select.list(QualityGateCondition::new);
      } catch (SQLException e) {
        throw new IllegalStateException(String.format("Fail to select quality gate conditions of quality gate %s", qualityGateId), e);
      }
    }

    private void updateQualityGateConditions(Collection<QualityGateCondition> conditions, String metricKey) throws SQLException {
      Upsert upsert = context.prepareUpsert("update quality_gate_conditions set metric_id=? where id=?");
      conditions.forEach(condition -> {
        try {
          upsert
            .setLong(1, metricsByMetricKeys.get(metricKey).getId())
            .setLong(2, condition.getId())
            .execute()
            .commit();
        } catch (SQLException e) {
          Throwables.propagate(e);
        }
      });
    }

    private void removeQualityGateConditions(Collection<QualityGateCondition> conditions) throws SQLException {
      if (conditions.isEmpty()) {
        return;
      }
      Upsert upsert = context.prepareUpsert("delete from quality_gate_conditions where id=?");
      conditions.forEach(condition -> {
        try {
          upsert.setLong(1, condition.getId())
            .execute()
            .commit();
        } catch (SQLException e) {
          Throwables.propagate(e);
        }
      });
    }
  }

  private static List<Metric> selectMetrics(Context context) throws SQLException {
    List<String> metricKeys = new ArrayList<>(COVERAGE_METRIC_KEYS);
    metricKeys.addAll(COVERAGE_METRIC_KEYS.stream().map(metricKey -> IT_PREFIX + metricKey).collect(Collectors.toList()));
    metricKeys.addAll(COVERAGE_METRIC_KEYS.stream().map(metricKey -> OVERALL_PREFIX + metricKey).collect(Collectors.toList()));
    metricKeys.addAll(metricKeys.stream().map(metricKey -> NEW_PREFIX + metricKey).collect(Collectors.toList()));
    Select select = context.prepareSelect("select id, name from metrics where name in (" + repeat("?", ",", metricKeys.size()) + ")");
    for (int i = 0; i < metricKeys.size(); i++) {
      select.setString(i + 1, metricKeys.get(i));
    }
    return select.list(Metric::new);
  }

  private static class QualityGateCondition {
    private final long id;
    private final long metricId;

    QualityGateCondition(Select.Row row) throws SQLException {
      this.id = requireNonNull(row.getLong(1));
      this.metricId = requireNonNull(row.getLong(2));
    }

    long getId() {
      return id;
    }

    long getMetricId() {
      return metricId;
    }
  }

  private static class Metric {
    private final long id;
    private final String key;

    Metric(Select.Row row) throws SQLException {
      this.id = requireNonNull(row.getLong(1));
      this.key = requireNonNull(row.getString(2));
    }

    long getId() {
      return id;
    }

    String getKey() {
      return key;
    }
  }
}
