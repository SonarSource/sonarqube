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
package org.sonar.server.qualitygate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import org.picocontainer.Startable;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.measure.Rating;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_GREATER_THAN;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_LESS_THAN;

public class RegisterQualityGates implements Startable {

  private static final Logger LOGGER = Loggers.get(RegisterQualityGates.class);

  private static final String BUILTIN_QUALITY_GATE_NAME = "Sonar way";
  private static final String A_RATING = Integer.toString(Rating.A.getIndex());

  private static final List<QualityGateCondition> QUALITY_GATE_CONDITIONS = asList(
    new QualityGateCondition().setMetricKey(NEW_SECURITY_RATING_KEY).setOperator(OPERATOR_GREATER_THAN).setErrorThreshold(A_RATING),
    new QualityGateCondition().setMetricKey(NEW_RELIABILITY_RATING_KEY).setOperator(OPERATOR_GREATER_THAN).setErrorThreshold(A_RATING),
    new QualityGateCondition().setMetricKey(NEW_MAINTAINABILITY_RATING_KEY).setOperator(OPERATOR_GREATER_THAN).setErrorThreshold(A_RATING),
    new QualityGateCondition().setMetricKey(NEW_COVERAGE_KEY).setOperator(OPERATOR_LESS_THAN).setErrorThreshold("80"),
    new QualityGateCondition().setMetricKey(NEW_DUPLICATED_LINES_DENSITY_KEY).setOperator(OPERATOR_GREATER_THAN).setErrorThreshold("3"));

  private final DbClient dbClient;
  private final QualityGateConditionsUpdater qualityGateConditionsUpdater;
  private final QualityGateDao qualityGateDao;
  private final QualityGateConditionDao qualityGateConditionDao;
  private final UuidFactory uuidFactory;
  private final System2 system2;

  public RegisterQualityGates(DbClient dbClient,
    QualityGateConditionsUpdater qualityGateConditionsUpdater, UuidFactory uuidFactory, System2 system2) {
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
      QualityGateDto builtin = qualityGateDao.selectByName(dbSession, BUILTIN_QUALITY_GATE_NAME);

      // Create builtin if not present
      if (builtin == null) {
        LOGGER.info("Built-in quality gate [{}] has been created", BUILTIN_QUALITY_GATE_NAME);
        builtin = createQualityGate(dbSession, BUILTIN_QUALITY_GATE_NAME);
      }

      // Set builtin if missing
      if (!builtin.isBuiltIn()) {
        builtin.setBuiltIn(true);
        dbClient.qualityGateDao().update(builtin, dbSession);
        LOGGER.info("Quality gate [{}] has been set as built-in", BUILTIN_QUALITY_GATE_NAME);
      }

      updateQualityConditionsIfRequired(dbSession, builtin);

      qualityGateDao.ensureOneBuiltInQualityGate(dbSession, BUILTIN_QUALITY_GATE_NAME);

      dbSession.commit();
    }
  }

  private void updateQualityConditionsIfRequired(DbSession dbSession, QualityGateDto builtin) {
    Map<Long, String> idToKeyMetric = dbClient.metricDao().selectAll(dbSession).stream()
      .collect(toMap(metricDto -> metricDto.getId().longValue(), MetricDto::getKey));

    List<QualityGateCondition> qualityGateConditions = qualityGateConditionDao.selectForQualityGate(dbSession, builtin.getId())
      .stream()
      .map(dto -> QualityGateCondition.from(dto, idToKeyMetric))
      .collect(MoreCollectors.toList());

    // Find all conditions that are not present in QUALITY_GATE_CONDITIONS
    // Those conditions must be deleted
    List<QualityGateCondition> qgConditionsToBeDeleted = new ArrayList<>(qualityGateConditions);
    qgConditionsToBeDeleted.removeAll(QUALITY_GATE_CONDITIONS);
    qgConditionsToBeDeleted
      .forEach(qgc -> qualityGateConditionDao.delete(qgc.toQualityGateDto(builtin.getId()), dbSession));

    // Find all conditions that are not present in qualityGateConditions
    // Those conditions must be created
    List<QualityGateCondition> qgConditionsToBeCreated = new ArrayList<>(QUALITY_GATE_CONDITIONS);
    qgConditionsToBeCreated.removeAll(qualityGateConditions);
    qgConditionsToBeCreated
      .forEach(qgc -> qualityGateConditionsUpdater.createCondition(dbSession, builtin, qgc.getMetricKey(), qgc.getOperator(),
        qgc.getErrorThreshold()));

    if (!qgConditionsToBeCreated.isEmpty() || !qgConditionsToBeDeleted.isEmpty()) {
      LOGGER.info("Built-in quality gate's conditions of [{}] has been updated", BUILTIN_QUALITY_GATE_NAME);
    }
  }

  @Override
  public void stop() {
    // do nothing
  }

  private QualityGateDto createQualityGate(DbSession dbSession, String name) {
    QualityGateDto qualityGate = new QualityGateDto()
      .setName(name)
      .setBuiltIn(true)
      .setUuid(uuidFactory.create())
      .setCreatedAt(new Date(system2.now()));
    return dbClient.qualityGateDao().insert(dbSession, qualityGate);
  }

  private static class QualityGateCondition {
    private Long id;
    private String metricKey;
    private String operator;
    private String errorThreshold;

    public static QualityGateCondition from(QualityGateConditionDto qualityGateConditionDto, Map<Long, String> mapping) {
      return new QualityGateCondition()
        .setId(qualityGateConditionDto.getId())
        .setMetricKey(mapping.get(qualityGateConditionDto.getMetricId()))
        .setOperator(qualityGateConditionDto.getOperator())
        .setErrorThreshold(qualityGateConditionDto.getErrorThreshold());
    }

    @CheckForNull
    public Long getId() {
      return id;
    }

    public QualityGateCondition setId(Long id) {
      this.id = id;
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

    public QualityGateConditionDto toQualityGateDto(long qualityGateId) {
      return new QualityGateConditionDto()
        .setId(id)
        .setMetricKey(metricKey)
        .setOperator(operator)
        .setErrorThreshold(errorThreshold)
        .setQualityGateId(qualityGateId);
    }

    // id does not belongs to equals to be able to be compared with builtin
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

    // id does not belongs to hashcode to be able to be compared with builtin
    @Override
    public int hashCode() {
      return Objects.hash(metricKey, operator, errorThreshold);
    }
  }
}
