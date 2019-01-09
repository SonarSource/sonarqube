/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v76;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.platform.db.migration.step.Select.LONG_READER;

@SupportsBlueGreen
public class MigrateNoMoreUsedQualityGateConditions extends DataChange {

  private static final Logger LOG = Loggers.get(MigrateNoMoreUsedQualityGateConditions.class);

  private static final String OPERATOR_GREATER_THAN = "GT";
  private static final String OPERATOR_LESS_THAN = "LT";

  private static final int DIRECTION_WORST = -1;
  private static final int DIRECTION_BETTER = 1;
  private static final int DIRECTION_NONE = 0;

  private static final Set<String> SUPPORTED_OPERATORS = ImmutableSet.of(OPERATOR_GREATER_THAN, OPERATOR_LESS_THAN);
  private static final Set<String> SUPPORTED_METRIC_TYPES = ImmutableSet.of("INT", "FLOAT", "PERCENT", "MILLISEC", "LEVEL", "RATING", "WORK_DUR");
  private static final Map<String, String> LEAK_METRIC_KEY_BY_METRIC_KEY = ImmutableMap.<String, String>builder()
    .put("branch_coverage", "new_branch_coverage")
    .put("conditions_to_cover", "new_conditions_to_cover")
    .put("coverage", "new_coverage")
    .put("line_coverage", "new_line_coverage")
    .put("lines_to_cover", "new_lines_to_cover")
    .put("uncovered_conditions", "new_uncovered_conditions")
    .put("uncovered_lines", "new_uncovered_lines")
    .put("duplicated_blocks", "new_duplicated_blocks")
    .put("duplicated_lines", "new_duplicated_lines")
    .put("duplicated_lines_density", "new_duplicated_lines_density")
    .put("blocker_violations", "new_blocker_violations")
    .put("critical_violations", "new_critical_violations")
    .put("info_violations", "new_info_violations")
    .put("violations", "new_violations")
    .put("major_violations", "new_major_violations")
    .put("minor_violations", "new_minor_violations")
    .put("sqale_index", "new_technical_debt")
    .put("code_smells", "new_code_smells")
    .put("sqale_rating", "new_maintainability_rating")
    .put("sqale_debt_ratio", "new_sqale_debt_ratio")
    .put("bugs", "new_bugs")
    .put("reliability_rating", "new_reliability_rating")
    .put("reliability_remediation_effort", "new_reliability_remediation_effort")
    .put("vulnerabilities", "new_vulnerabilities")
    .put("security_rating", "new_security_rating")
    .put("security_remediation_effort", "new_security_remediation_effort")
    .put("lines", "new_lines")
    .build();

  private final System2 system2;

  public MigrateNoMoreUsedQualityGateConditions(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MigrationContext migrationContext = new MigrationContext(context, new Date(system2.now()), loadMetrics(context));
    List<Long> qualityGateIds = context.prepareSelect("SELECT id FROM quality_gates qg WHERE qg.is_built_in=?")
      .setBoolean(1, false)
      .list(LONG_READER);
    for (long qualityGateId : qualityGateIds) {
      List<QualityGateCondition> conditions = loadConditions(context, qualityGateId);

      markNoMoreSupportedConditionsAsToBeDeleted(migrationContext, conditions);
      markConditionsHavingOnlyWarningAsToBeDeleted(conditions);
      markConditionsUsingLeakPeriodHavingNoRelatedLeakMetricAsToBeDeleted(migrationContext, conditions);
      markConditionsUsingLeakPeriodHavingAlreadyRelatedConditionAsToBeDeleted(migrationContext, conditions);
      updateConditionsUsingLeakPeriod(migrationContext, conditions);
      updateConditionsHavingErrorAndWarningByRemovingWarning(migrationContext, conditions);
      dropConditionsIfNeeded(migrationContext, conditions);

      migrationContext.increaseNumberOfProcessedQualityGate();
    }
    LOG.info("{} custom quality gates have been loaded", migrationContext.getNbOfQualityGates());
    LOG.info("{} conditions have been removed", migrationContext.getNbOfRemovedConditions());
    LOG.info("{} conditions have been updated", migrationContext.getNbOfUpdatedConditions());
  }

  private static List<Metric> loadMetrics(Context context) throws SQLException {
    return context
      .prepareSelect("SELECT m.id, m.name, m.val_type, m.direction FROM metrics m WHERE m.enabled=?")
      .setBoolean(1, true)
      .list(row -> new Metric(row.getInt(1), row.getString(2), row.getString(3), row.getInt(4)));
  }

  private static List<QualityGateCondition> loadConditions(Context context, long qualityGateId) throws SQLException {
    return context.prepareSelect("SELECT qgc.id, qgc.metric_id, qgc.operator, qgc.value_error, qgc.value_warning, qgc.period FROM quality_gate_conditions qgc " +
      "WHERE qgc.qgate_id=? ")
      .setLong(1, qualityGateId)
      .list(
        row -> new QualityGateCondition(row.getInt(1), row.getInt(2), row.getString(3),
          row.getString(4), row.getString(5), row.getInt(6)));
  }

  private static void markNoMoreSupportedConditionsAsToBeDeleted(MigrationContext migrationContext, List<QualityGateCondition> conditions) {
    conditions.stream()
      .filter(c -> !c.isToBeDeleted())
      .filter(c -> !isConditionStillSupported(c, migrationContext.getMetricById(c.getMetricId())))
      .forEach(QualityGateCondition::setToBeDeleted);
  }

  private static void markConditionsHavingOnlyWarningAsToBeDeleted(List<QualityGateCondition> conditions) {
    conditions.stream()
      .filter(c -> !c.isToBeDeleted())
      .filter(c -> !isNullOrEmpty(c.getWarning()) && isNullOrEmpty(c.getError()))
      .forEach(QualityGateCondition::setToBeDeleted);
  }

  private static void markConditionsUsingLeakPeriodHavingNoRelatedLeakMetricAsToBeDeleted(MigrationContext migrationContext, List<QualityGateCondition> conditions) {
    conditions
      .stream()
      .filter(c -> !c.isToBeDeleted())
      .filter(QualityGateCondition::hasLeakPeriod)
      .filter(condition -> !isConditionOnLeakMetric(migrationContext, condition))
      .forEach(condition -> {
        String metricKey = migrationContext.getMetricById(condition.getMetricId()).getKey();
        String relatedLeakMetric = LEAK_METRIC_KEY_BY_METRIC_KEY.get(metricKey);
        // Metric has no related metric on leak period => delete condition
        if (relatedLeakMetric == null) {
          condition.setToBeDeleted();
        }
      });
  }

  private static void markConditionsUsingLeakPeriodHavingAlreadyRelatedConditionAsToBeDeleted(MigrationContext migrationContext, List<QualityGateCondition> conditions) {
    Map<String, QualityGateCondition> conditionsByMetricKey = conditions.stream()
      .filter(c -> !c.isToBeDeleted())
      .collect(uniqueIndex(c -> migrationContext.getMetricById(c.getMetricId()).getKey()));

    conditions
      .stream()
      .filter(condition -> !condition.isToBeDeleted())
      .filter(QualityGateCondition::hasLeakPeriod)
      .filter(condition -> !isConditionOnLeakMetric(migrationContext, condition))
      .forEach(condition -> {
        String metricKey = migrationContext.getMetricById(condition.getMetricId()).getKey();
        String relatedLeakMetric = LEAK_METRIC_KEY_BY_METRIC_KEY.get(metricKey);
        if (relatedLeakMetric != null) {
          QualityGateCondition existingConditionUsingRelatedLeakPeriod = conditionsByMetricKey.get(relatedLeakMetric);
          if (existingConditionUsingRelatedLeakPeriod != null) {
            // Another condition on related leak period metric exist => delete condition
            condition.setToBeDeleted();
          }
        }
      });
  }

  private static void updateConditionsHavingErrorAndWarningByRemovingWarning(MigrationContext migrationContext, List<QualityGateCondition> conditions)
    throws SQLException {
    Set<Integer> conditionsToBeUpdated = conditions.stream()
      .filter(c -> !c.isToBeDeleted())
      .filter(c -> !isNullOrEmpty(c.getWarning()) && !isNullOrEmpty(c.getError()))
      .map(QualityGateCondition::getId)
      .collect(toSet());
    if (conditionsToBeUpdated.isEmpty()) {
      return;
    }
    migrationContext.getContext()
      .prepareUpsert("UPDATE quality_gate_conditions SET value_warning = NULL, updated_at = ? WHERE id IN (" + conditionsToBeUpdated
        .stream()
        .map(c -> Integer.toString(c))
        .collect(Collectors.joining(",")) + ")")
      .setDate(1, migrationContext.getNow())
      .execute()
      .commit();
    migrationContext.addUpdatedConditions(conditionsToBeUpdated.size());
  }

  private static void updateConditionsUsingLeakPeriod(MigrationContext migrationContext, List<QualityGateCondition> conditions)
    throws SQLException {

    Map<String, QualityGateCondition> conditionsByMetricKey = conditions.stream()
      .filter(c -> !c.isToBeDeleted())
      .collect(uniqueIndex(c -> migrationContext.getMetricById(c.getMetricId()).getKey()));

    Upsert updateMetricId = migrationContext.getContext()
      .prepareUpsert("UPDATE quality_gate_conditions SET metric_id = ?, updated_at = ? WHERE id = ? ")
      .setDate(2, migrationContext.getNow());

    conditions
      .stream()
      .filter(c -> !c.isToBeDeleted())
      .filter(QualityGateCondition::hasLeakPeriod)
      .filter(condition -> !isConditionOnLeakMetric(migrationContext, condition))
      .forEach(condition -> {
        String metricKey = migrationContext.getMetricById(condition.getMetricId()).getKey();
        String relatedLeakMetric = LEAK_METRIC_KEY_BY_METRIC_KEY.get(metricKey);
        QualityGateCondition existingConditionUsingRelatedLeakPeriod = conditionsByMetricKey.get(relatedLeakMetric);
        // Metric has a related leak period metric => update the condition
        if (existingConditionUsingRelatedLeakPeriod == null) {
          try {
            updateMetricId.setInt(1, migrationContext.getMetricByKey(relatedLeakMetric).getId());
            updateMetricId.setInt(3, condition.getId());
            updateMetricId.execute();
            migrationContext.addUpdatedConditions(1);
          } catch (SQLException e) {
            throw new IllegalStateException("Fail to update quality gate conditions", e);
          }
        }
      });
    updateMetricId.commit();
  }

  private static void dropConditionsIfNeeded(MigrationContext context, List<QualityGateCondition> conditions) throws SQLException {
    List<QualityGateCondition> conditionsToBeDeleted = conditions.stream()
      .filter(QualityGateCondition::isToBeDeleted)
      .collect(toList());
    if (conditionsToBeDeleted.isEmpty()) {
      return;
    }
    context.getContext()
      .prepareUpsert("DELETE FROM quality_gate_conditions WHERE id IN (" + conditionsToBeDeleted
        .stream()
        .map(c -> Integer.toString(c.getId()))
        .collect(Collectors.joining(",")) + ")")
      .execute()
      .commit();
    context.addRemovedConditions(conditionsToBeDeleted.size());
  }

  private static boolean isConditionOnLeakMetric(MigrationContext migrationContext, QualityGateCondition condition) {
    return LEAK_METRIC_KEY_BY_METRIC_KEY.containsValue(migrationContext.getMetricById(condition.getMetricId()).getKey());
  }

  private static boolean isConditionStillSupported(QualityGateCondition condition, Metric metric) {
    return isSupportedMetricType(metric) && isSupportedOperator(condition, metric);
  }

  private static boolean isSupportedMetricType(Metric metric) {
    return SUPPORTED_METRIC_TYPES.contains(metric.getType());
  }

  private static boolean isSupportedOperator(QualityGateCondition condition, Metric metric) {
    String operator = condition.getOperator();
    int direction = metric.getDirection();
    return SUPPORTED_OPERATORS.contains(operator) &&
      (direction == DIRECTION_NONE ||
        (direction == DIRECTION_WORST && operator.equalsIgnoreCase(OPERATOR_GREATER_THAN)) ||
        (direction == DIRECTION_BETTER && operator.equalsIgnoreCase(OPERATOR_LESS_THAN)));
  }

  private static class QualityGateCondition {
    private final int id;
    private final int metricId;
    private final String operator;
    private final String error;
    private final String warning;
    private final Integer period;

    private boolean toBeDeleted = false;

    public QualityGateCondition(int id, int metricId, String operator, @Nullable String error, @Nullable String warning,
      @Nullable Integer period) {
      this.id = id;
      this.metricId = metricId;
      this.operator = operator;
      this.error = error;
      this.warning = warning;
      this.period = period;
    }

    public int getId() {
      return id;
    }

    public int getMetricId() {
      return metricId;
    }

    public String getOperator() {
      return operator;
    }

    @CheckForNull
    public String getError() {
      return error;
    }

    @CheckForNull
    public String getWarning() {
      return warning;
    }

    public boolean hasLeakPeriod() {
      return period != null && period == 1;
    }

    public void setToBeDeleted() {
      toBeDeleted = true;
    }

    public boolean isToBeDeleted() {
      return toBeDeleted;
    }
  }

  private static class Metric {
    private final int id;
    private final String key;
    private final String type;
    private final int direction;

    public Metric(int id, String key, String type, int direction) {
      this.id = id;
      this.key = key;
      this.type = type;
      this.direction = direction;
    }

    public int getId() {
      return id;
    }

    public String getKey() {
      return key;
    }

    public String getType() {
      return type;
    }

    public int getDirection() {
      return direction;
    }
  }

  private static class MigrationContext {

    private final Context context;
    private final Date now;
    private final Map<Integer, Metric> metricsById;
    private final Map<String, Metric> metricsByKey;

    private int nbOfQualityGates;
    private int nbOfRemovedConditions;
    private int nbOfUpdatedConditions;

    public MigrationContext(Context context, Date now, List<Metric> metrics) {
      this.context = context;
      this.now = now;
      this.metricsById = metrics.stream().collect(uniqueIndex(Metric::getId));
      this.metricsByKey = metrics.stream().collect(uniqueIndex(Metric::getKey));
    }

    public Context getContext() {
      return context;
    }

    public Date getNow() {
      return now;
    }

    public Metric getMetricByKey(String key) {
      return metricsByKey.get(key);
    }

    public Metric getMetricById(int id) {
      return metricsById.get(id);
    }

    public void increaseNumberOfProcessedQualityGate() {
      nbOfQualityGates += 1;
    }

    public int getNbOfQualityGates() {
      return nbOfQualityGates;
    }

    public void addRemovedConditions(int removedConditions) {
      nbOfRemovedConditions += removedConditions;
    }

    public int getNbOfRemovedConditions() {
      return nbOfRemovedConditions;
    }

    public void addUpdatedConditions(int updatedConditions) {
      nbOfUpdatedConditions += updatedConditions;
    }

    public int getNbOfUpdatedConditions() {
      return nbOfUpdatedConditions;
    }
  }

}
