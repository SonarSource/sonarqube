/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.qualitygate;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.measure.Rating;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_VIOLATIONS_KEY;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_GREATER_THAN;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_LESS_THAN;
import static org.sonar.server.qualitygate.QualityGate.BUILTIN_QUALITY_GATE_NAME;
import static org.sonar.server.qualitygate.QualityGate.SONAR_WAY_LEGACY_QUALITY_GATE_NAME;

public class RegisterQualityGates implements Startable {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterQualityGates.class);

  private static final String A_RATING = Integer.toString(Rating.A.getIndex());
  private static final List<QualityGateCondition> BUILT_IN_QUALITY_GATE_CONDITIONS = asList(
    new QualityGateCondition().setMetricKey(NEW_VIOLATIONS_KEY).setOperator(OPERATOR_GREATER_THAN).setErrorThreshold("0"),
    new QualityGateCondition().setMetricKey(NEW_COVERAGE_KEY).setOperator(OPERATOR_LESS_THAN).setErrorThreshold("80"),
    new QualityGateCondition().setMetricKey(NEW_DUPLICATED_LINES_DENSITY_KEY).setOperator(OPERATOR_GREATER_THAN).setErrorThreshold("3"),
    new QualityGateCondition().setMetricKey(NEW_SECURITY_HOTSPOTS_REVIEWED_KEY).setOperator(OPERATOR_LESS_THAN).setErrorThreshold("100"));

  private static final List<QualityGateCondition> SONAR_WAY_LEGACY_QUALITY_GATE_CONDITIONS = asList(
    new QualityGateCondition().setMetricKey(NEW_SECURITY_RATING_KEY).setOperator(OPERATOR_GREATER_THAN).setErrorThreshold(A_RATING),
    new QualityGateCondition().setMetricKey(NEW_RELIABILITY_RATING_KEY).setOperator(OPERATOR_GREATER_THAN).setErrorThreshold(A_RATING),
    new QualityGateCondition().setMetricKey(NEW_MAINTAINABILITY_RATING_KEY).setOperator(OPERATOR_GREATER_THAN).setErrorThreshold(A_RATING),
    new QualityGateCondition().setMetricKey(NEW_COVERAGE_KEY).setOperator(OPERATOR_LESS_THAN).setErrorThreshold("80"),
    new QualityGateCondition().setMetricKey(NEW_DUPLICATED_LINES_DENSITY_KEY).setOperator(OPERATOR_GREATER_THAN).setErrorThreshold("3"),
    new QualityGateCondition().setMetricKey(NEW_SECURITY_HOTSPOTS_REVIEWED_KEY).setOperator(OPERATOR_LESS_THAN).setErrorThreshold("100"));

  private final DbClient dbClient;
  private final QualityGateConditionsUpdater qualityGateConditionsUpdater;
  private final QualityGateDao qualityGateDao;
  private final QualityGateConditionDao qualityGateConditionDao;
  private final UuidFactory uuidFactory;
  private final System2 system2;

  public RegisterQualityGates(DbClient dbClient, QualityGateConditionsUpdater qualityGateConditionsUpdater, UuidFactory uuidFactory, System2 system2) {
    this.dbClient = dbClient;
    this.qualityGateConditionsUpdater = qualityGateConditionsUpdater;
    this.qualityGateDao = dbClient.qualityGateDao();
    this.qualityGateConditionDao = dbClient.gateConditionDao();
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityGateDto builtinQualityGate = qualityGateDao.selectByName(dbSession, BUILTIN_QUALITY_GATE_NAME);

      // Create builtinQualityGate if not present
      if (builtinQualityGate == null) {
        LOGGER.info("Built-in quality gate [{}] has been created", BUILTIN_QUALITY_GATE_NAME);
        builtinQualityGate = createQualityGate(dbSession, BUILTIN_QUALITY_GATE_NAME, true);
      }
      List<QualityGateCondition> builtInQualityGateConditions = getQualityGateConditions(dbSession, builtinQualityGate);

      // Create sonar way (legacy) only if it is not a new instance (a new instance has a Sonar way QG and no conditions) and if it is
      // not already present
      if (!builtInQualityGateConditions.isEmpty()) {
        createSonarWayLegacyQualityGateIfMissing(dbSession);
      }


      // Set builtinQualityGate if missing
      if (!builtinQualityGate.isBuiltIn()) {
        builtinQualityGate.setBuiltIn(true);
        dbClient.qualityGateDao().update(builtinQualityGate, dbSession);
        LOGGER.info("Quality gate [{}] has been set as built-in", BUILTIN_QUALITY_GATE_NAME);
      }

      updateQualityConditionsIfRequired(dbSession, builtinQualityGate, builtInQualityGateConditions);

      qualityGateDao.ensureOneBuiltInQualityGate(dbSession, BUILTIN_QUALITY_GATE_NAME);

      dbSession.commit();
    }
  }

  private void createSonarWayLegacyQualityGateIfMissing(DbSession dbSession) {
    QualityGateDto sonarWayLegacyQualityGate = qualityGateDao.selectByName(dbSession, SONAR_WAY_LEGACY_QUALITY_GATE_NAME);
    LOGGER.info("Sonar way legacy Gate: {} ", sonarWayLegacyQualityGate);
    if (sonarWayLegacyQualityGate == null) {
      sonarWayLegacyQualityGate = createQualityGate(dbSession, SONAR_WAY_LEGACY_QUALITY_GATE_NAME, false);
      addConditionsToQualityGate(dbSession, sonarWayLegacyQualityGate, SONAR_WAY_LEGACY_QUALITY_GATE_CONDITIONS);
      LOGGER.info("Sonar way (legacy) quality gate has been created");
    }
  }

  private void updateQualityConditionsIfRequired(DbSession dbSession, QualityGateDto builtinQualityGate,
    List<QualityGateCondition> qualityGateConditions) {

    List<QualityGateCondition> qgConditionsDeleted = removeExtraConditions(dbSession, builtinQualityGate, qualityGateConditions);
    qgConditionsDeleted.addAll(removeDuplicatedConditions(dbSession, builtinQualityGate, qualityGateConditions));

    List<QualityGateCondition> qgConditionsAdded = addMissingConditions(dbSession, builtinQualityGate, qualityGateConditions);

    if (!qgConditionsAdded.isEmpty() || !qgConditionsDeleted.isEmpty()) {
      LOGGER.info("Built-in quality gate's conditions of [{}] has been updated", BUILTIN_QUALITY_GATE_NAME);
    }
  }

  private ImmutableList<QualityGateCondition> getQualityGateConditions(DbSession dbSession, QualityGateDto builtinQualityGate) {
    Map<String, String> uuidToKeyMetric = dbClient.metricDao().selectAll(dbSession).stream()
      .collect(toMap(MetricDto::getUuid, MetricDto::getKey));

    return qualityGateConditionDao.selectForQualityGate(dbSession, builtinQualityGate.getUuid())
      .stream()
      .map(dto -> QualityGateCondition.from(dto, uuidToKeyMetric))
      .collect(toImmutableList());
  }

  private List<QualityGateCondition> removeExtraConditions(DbSession dbSession, QualityGateDto builtinQualityGate, List<QualityGateCondition> qualityGateConditions) {
    // Find all conditions that are not present in QUALITY_GATE_CONDITIONS
    // Those conditions must be deleted
    List<QualityGateCondition> qgConditionsToBeDeleted = new ArrayList<>(qualityGateConditions);
    qgConditionsToBeDeleted.removeAll(BUILT_IN_QUALITY_GATE_CONDITIONS);
    qgConditionsToBeDeleted
      .forEach(qgc -> qualityGateConditionDao.delete(qgc.toQualityGateDto(builtinQualityGate.getUuid()), dbSession));
    return qgConditionsToBeDeleted;
  }

  private Set<QualityGateCondition> removeDuplicatedConditions(DbSession dbSession, QualityGateDto builtinQualityGate, List<QualityGateCondition> qualityGateConditions) {
    Set<QualityGateCondition> qgConditionsDuplicated = qualityGateConditions
      .stream()
      .filter(qualityGateCondition -> Collections.frequency(qualityGateConditions, qualityGateCondition) > 1)
      .collect(Collectors.toSet());

    qgConditionsDuplicated
      .forEach(qgc -> qualityGateConditionDao.delete(qgc.toQualityGateDto(builtinQualityGate.getUuid()), dbSession));

    return qgConditionsDuplicated;
  }

  private List<QualityGateCondition> addMissingConditions(DbSession dbSession, QualityGateDto builtinQualityGate,
    List<QualityGateCondition> qualityGateConditions) {
    // Find all conditions that are not present in qualityGateConditions
    // Those conditions must be added to the built-in quality gate
    List<QualityGateCondition> qgConditionsToBeAdded = new ArrayList<>(BUILT_IN_QUALITY_GATE_CONDITIONS);
    qgConditionsToBeAdded.removeAll(qualityGateConditions);
    addConditionsToQualityGate(dbSession, builtinQualityGate, qgConditionsToBeAdded);
    return qgConditionsToBeAdded;
  }

  private void addConditionsToQualityGate(DbSession dbSession, QualityGateDto qualityGate, List<QualityGateCondition> conditions) {
    conditions.forEach(condition -> qualityGateConditionsUpdater.createCondition(dbSession, qualityGate, condition.getMetricKey(),
      condition.getOperator(),
      condition.getErrorThreshold()));
  }

  @Override
  public void stop() {
    // do nothing
  }

  private QualityGateDto createQualityGate(DbSession dbSession, String name, boolean isBuiltIn) {
    QualityGateDto qualityGate = new QualityGateDto()
      .setName(name)
      .setBuiltIn(isBuiltIn)
      .setUuid(uuidFactory.create())
      .setCreatedAt(new Date(system2.now()));
    return dbClient.qualityGateDao().insert(dbSession, qualityGate);
  }

  private static class QualityGateCondition {
    private String uuid;
    private String metricKey;
    private String operator;
    private String errorThreshold;

    public static QualityGateCondition from(QualityGateConditionDto qualityGateConditionDto, Map<String, String> mapping) {
      return new QualityGateCondition()
        .setUuid(qualityGateConditionDto.getUuid())
        .setMetricKey(mapping.get(qualityGateConditionDto.getMetricUuid()))
        .setOperator(qualityGateConditionDto.getOperator())
        .setErrorThreshold(qualityGateConditionDto.getErrorThreshold());
    }

    @CheckForNull
    public String getUuid() {
      return uuid;
    }

    public QualityGateCondition setUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public String getMetricKey() {
      return metricKey;
    }

    public QualityGateCondition setMetricKey(String metricKey) {
      this.metricKey = metricKey;
      return this;
    }

    public String getOperator() {
      return operator;
    }

    public QualityGateCondition setOperator(String operator) {
      this.operator = operator;
      return this;
    }

    public String getErrorThreshold() {
      return errorThreshold;
    }

    public QualityGateCondition setErrorThreshold(String errorThreshold) {
      this.errorThreshold = errorThreshold;
      return this;
    }

    public QualityGateConditionDto toQualityGateDto(String qualityGateUuid) {
      return new QualityGateConditionDto()
        .setUuid(uuid)
        .setMetricKey(metricKey)
        .setOperator(operator)
        .setErrorThreshold(errorThreshold)
        .setQualityGateUuid(qualityGateUuid);
    }

    // id does not belong to equals to be able to be compared with builtin
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      QualityGateCondition that = (QualityGateCondition) o;
      return Objects.equals(metricKey, that.metricKey) &&
        Objects.equals(operator, that.operator) &&
        Objects.equals(errorThreshold, that.errorThreshold);
    }

    // id does not belong to hashcode to be able to be compared with builtin
    @Override
    public int hashCode() {
      return Objects.hash(metricKey, operator, errorThreshold);
    }
  }
}
