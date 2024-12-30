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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.apache.commons.lang3.StringUtils;
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
import org.sonar.server.qualitygate.builtin.BuiltInQualityGate;
import org.sonar.server.qualitygate.builtin.SonarWayQualityGate;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_GREATER_THAN;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_LESS_THAN;

public class RegisterQualityGates implements Startable {

  static final String SONAR_WAY_LEGACY_NAME = "Sonar way (legacy)";

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterQualityGates.class);

  private final DbClient dbClient;
  private final Map<String, BuiltInQualityGate> builtInQualityGateMap;
  private final QualityGateConditionsUpdater qualityGateConditionsUpdater;
  private final QualityGateDao qualityGateDao;
  private final QualityGateConditionDao qualityGateConditionDao;
  private final UuidFactory uuidFactory;
  private final System2 system2;

  public RegisterQualityGates(DbClient dbClient, BuiltInQualityGate[] builtInQualityGates, QualityGateConditionsUpdater qualityGateConditionsUpdater,
    UuidFactory uuidFactory, System2 system2) {
    this.dbClient = dbClient;
    this.builtInQualityGateMap = Stream.of(builtInQualityGates)
      .collect(Collectors.toMap(BuiltInQualityGate::getName, Function.identity()));
    this.qualityGateConditionsUpdater = qualityGateConditionsUpdater;
    this.qualityGateDao = dbClient.qualityGateDao();
    this.qualityGateConditionDao = dbClient.gateConditionDao();
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<QualityGateDto> builtinQualityGates = qualityGateDao.selectByNames(dbSession, builtInQualityGateMap.keySet());

      List<QualityGateDto> newBuiltinQualityGates = createBuiltInQualityGates(dbSession, builtinQualityGates);
      builtinQualityGates.addAll(newBuiltinQualityGates);

      createLegacyQualityGate(dbSession, builtinQualityGates);
      updateQualityGates(dbSession, builtinQualityGates);
      cleanupQualityGates(dbSession);

      dbSession.commit();
    }
  }

  private List<QualityGateDto> createBuiltInQualityGates(DbSession dbSession, List<QualityGateDto> builtinQualityGates) {
    Set<String> builtinQualityGatesFromDB = builtinQualityGates.stream().map(QualityGateDto::getName).collect(Collectors.toSet());

    Set<String> qualityGatesToBeCreated = new HashSet<>(builtInQualityGateMap.keySet());
    qualityGatesToBeCreated.removeAll(builtinQualityGatesFromDB);

    return qualityGatesToBeCreated
      .stream().map(name -> {
        BuiltInQualityGate builtInQualityGateDef = builtInQualityGateMap.get(name);
        QualityGateDto qualityGate = createQualityGate(dbSession, builtInQualityGateDef.getName(), true, builtInQualityGateDef.supportsAiCode());
        LOGGER.info("Quality Gate [{}] has been created", qualityGate.getName());
        return qualityGate;
      })
      .toList();
  }

  private void createLegacyQualityGate(DbSession dbSession, List<QualityGateDto> builtinQualityGates) {
    Optional<QualityGateDto> existingSonarWay = builtinQualityGates.stream().filter(qg -> SonarWayQualityGate.NAME.equals(qg.getName())).findFirst();

    if (existingSonarWay.isEmpty()) {
      return;
    }

    // Create sonar way (legacy) only if it is not a new instance (a new instance has a Sonar way QG and no conditions) and if it is
    // not already present
    // FIXME:: The logic explained above doesn't make any sense as after upgrade - legacy Quality Gate will created,
    // FIXME:: There is open bug ticket to address this issue: SONAR-23753
    boolean shouldCreateLegacy = qualityGateConditionDao.countByQualityGateUuid(dbSession, existingSonarWay.get().getUuid()) > 0;

    if (!shouldCreateLegacy) {
      return;
    }

    QualityGateDto sonarWayLegacyQualityGate = qualityGateDao.selectByName(dbSession, SONAR_WAY_LEGACY_NAME);

    if (sonarWayLegacyQualityGate == null) {
      sonarWayLegacyQualityGate = createQualityGate(dbSession, SONAR_WAY_LEGACY_NAME, false, false);
      addConditionsToQualityGate(dbSession, sonarWayLegacyQualityGate, asList(
        new QualityGateCondition().setMetricKey(NEW_SECURITY_RATING_KEY).setOperator(OPERATOR_GREATER_THAN).setErrorThreshold(Integer.toString(Rating.A.getIndex())),
        new QualityGateCondition().setMetricKey(NEW_RELIABILITY_RATING_KEY).setOperator(OPERATOR_GREATER_THAN).setErrorThreshold(Integer.toString(Rating.A.getIndex())),
        new QualityGateCondition().setMetricKey(NEW_MAINTAINABILITY_RATING_KEY).setOperator(OPERATOR_GREATER_THAN).setErrorThreshold(Integer.toString(Rating.A.getIndex())),
        new QualityGateCondition().setMetricKey(NEW_COVERAGE_KEY).setOperator(OPERATOR_LESS_THAN).setErrorThreshold("80"),
        new QualityGateCondition().setMetricKey(NEW_DUPLICATED_LINES_DENSITY_KEY).setOperator(OPERATOR_GREATER_THAN).setErrorThreshold("3"),
        new QualityGateCondition().setMetricKey(NEW_SECURITY_HOTSPOTS_REVIEWED_KEY).setOperator(OPERATOR_LESS_THAN).setErrorThreshold("100")));
      LOGGER.info("Sonar way (legacy) Quality Gate has been created");
    } else {
      LOGGER.info("Sonar way legacy Gate uuid: {} ", sonarWayLegacyQualityGate.getUuid());
    }
  }

  private void updateQualityGates(DbSession dbSession, List<QualityGateDto> builtinQualityGates) {
    Map<String, String> uuidToKeyMetric = dbClient.metricDao().selectAll(dbSession).stream()
      .collect(toMap(MetricDto::getUuid, MetricDto::getKey));

    builtinQualityGates.forEach(qualityGate -> {
      updateQualityConditionsIfRequired(dbSession, qualityGate, uuidToKeyMetric);
      if (!qualityGate.isBuiltIn()) {
        qualityGate.setBuiltIn(true);
        qualityGateDao.update(qualityGate, dbSession);
        LOGGER.info("Quality Gate [{}] builtin flag has been updated to [{}]", qualityGate.getName(), true);
      }
    });
  }

  private void updateQualityConditionsIfRequired(DbSession dbSession, QualityGateDto builtinQualityGate, Map<String, String> uuidToKeyMetric) {
    List<QualityGateCondition> qualityGateConditions = getQualityGateConditions(dbSession, builtinQualityGate, uuidToKeyMetric);

    List<QualityGateCondition> qgConditionsDeleted = removeExtraConditions(dbSession, builtinQualityGate, qualityGateConditions);
    qgConditionsDeleted.addAll(removeDuplicatedConditions(dbSession, builtinQualityGate, qualityGateConditions));

    List<QualityGateCondition> qgConditionsAdded = addMissingConditions(dbSession, builtinQualityGate, qualityGateConditions);

    if (!qgConditionsAdded.isEmpty() || !qgConditionsDeleted.isEmpty()) {
      LOGGER.info("Quality Gate's conditions of [{}] has been updated", builtinQualityGate.getName());
    }
  }

  private List<QualityGateCondition> getQualityGateConditions(DbSession dbSession, QualityGateDto builtinQualityGate, Map<String, String> uuidToKeyMetric) {
    return qualityGateConditionDao.selectForQualityGate(dbSession, builtinQualityGate.getUuid())
      .stream()
      .map(dto -> QualityGateCondition.from(dto, uuidToKeyMetric))
      .toList();
  }

  private List<QualityGateCondition> removeExtraConditions(DbSession dbSession, QualityGateDto builtinQualityGate, List<QualityGateCondition> qualityGateConditions) {
    List<QualityGateCondition> qgConditionsToBeDeleted = new ArrayList<>(qualityGateConditions);
    List<QualityGateCondition> gateConditions = builtInQualityGateMap.get(builtinQualityGate.getName()).getConditions().stream()
      .map(conditionDef -> new QualityGateCondition()
        .setMetricKey(conditionDef.getMetricKey())
        .setOperator(conditionDef.getOperator().getDbValue())
        .setErrorThreshold(conditionDef.getErrorThreshold()))
      .toList();
    qgConditionsToBeDeleted.removeAll(gateConditions);
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
    List<QualityGateCondition> gateConditions = builtInQualityGateMap.get(builtinQualityGate.getName()).getConditions().stream()
      .map(conditionDef -> new QualityGateCondition()
        .setMetricKey(conditionDef.getMetricKey())
        .setOperator(conditionDef.getOperator().getDbValue())
        .setErrorThreshold(conditionDef.getErrorThreshold()))
      .toList();
    List<QualityGateCondition> qgConditionsToBeAdded = new ArrayList<>(gateConditions);
    qgConditionsToBeAdded.removeAll(qualityGateConditions);
    addConditionsToQualityGate(dbSession, builtinQualityGate, qgConditionsToBeAdded);
    return qgConditionsToBeAdded;
  }

  private void addConditionsToQualityGate(DbSession dbSession, QualityGateDto qualityGate, List<QualityGateCondition> conditions) {
    conditions.forEach(condition -> qualityGateConditionsUpdater.createCondition(dbSession, qualityGate, condition.getMetricKey(),
      condition.getOperator(),
      condition.getErrorThreshold()));
  }

  private void cleanupQualityGates(DbSession dbSession) {
    List<QualityGateDto> qualityGateDtos = qualityGateDao.selectBuiltIn(dbSession);
    for (QualityGateDto builtinQualityGate : qualityGateDtos) {
      String oldName = builtinQualityGate.getName();
      if (!builtInQualityGateMap.containsKey(oldName)) {
        String newName = generateNewName(oldName);
        builtinQualityGate
          .setName(newName)
          .setAiCodeSupported(false)
          .setBuiltIn(false);
        qualityGateDao.update(builtinQualityGate, dbSession);
        LOGGER.info("Quality Gate [{}] has been unset as builtin and renamed to: [{}]", oldName, newName);
      }
    }
  }

  /**
   * Abbreviate Quality Gate name if it will be too long with prefix and append suffix
   */
  private String generateNewName(String name) {
    var shortName = StringUtils.abbreviate(name, 40);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd yyyy 'at' hh:mm a")
      .withLocale(Locale.getDefault())
      .withZone(ZoneId.systemDefault());
    var now = formatter.format(Instant.ofEpochMilli(system2.now()));
    String suffix = " (backup from " + now + ")";
    return shortName + suffix;
  }

  @Override
  public void stop() {
    // do nothing
  }

  private QualityGateDto createQualityGate(DbSession dbSession, String name, boolean isBuiltIn, boolean aiCodeSupported) {
    QualityGateDto qualityGate = new QualityGateDto()
      .setName(name)
      .setBuiltIn(isBuiltIn)
      .setAiCodeSupported(aiCodeSupported)
      .setUuid(uuidFactory.create())
      .setCreatedAt(new Date(system2.now()));
    return qualityGateDao.insert(dbSession, qualityGate);
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
