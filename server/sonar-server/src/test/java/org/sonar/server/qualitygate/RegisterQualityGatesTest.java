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
package org.sonar.server.qualitygate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDao;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.PERCENT;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_GREATER_THAN;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_LESS_THAN;

public class RegisterQualityGatesTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public LogTester logTester = new LogTester();

  private static final int LEAK_PERIOD = 1;
  private static final String BUILT_IN_NAME = "Sonar way";

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private QualityGateDao qualityGateDao = dbClient.qualityGateDao();
  private QualityGateConditionDao gateConditionDao = dbClient.gateConditionDao();
  private MetricDao metricDao = dbClient.metricDao();
  private QualityGateConditionsUpdater qualityGateConditionsUpdater = new QualityGateConditionsUpdater(dbClient);
  private QualityGateFinder qualityGateFinder = new QualityGateFinder(dbClient);

  private RegisterQualityGates underTest = new RegisterQualityGates(dbClient, qualityGateConditionsUpdater,
    UuidFactoryFast.getInstance(), System2.INSTANCE);

  @Test
  public void register_default_gate() {
    insertMetrics();

    underTest.start();

    verifyCorrectBuiltInQualityGate();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate [Sonar way] has been created")).isTrue();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate's conditions of [Sonar way] has been updated")).isTrue();
  }

  @Test
  public void upgrade_empty_quality_gate() {
    insertMetrics();

    underTest.start();

    assertThat(db.countRowsOfTable("quality_gates")).isEqualTo(1);
    verifyCorrectBuiltInQualityGate();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate's conditions of [Sonar way] has been updated")).isTrue();
  }

  @Test
  public void upgrade_should_remove_deleted_condition() {
    insertMetrics();
    QualityGateDto builtInQualityGate = db.qualityGates().insertBuiltInQualityGate();
    createBuiltInConditions(builtInQualityGate);
    // Add another condition
    qualityGateConditionsUpdater.createCondition(dbSession, builtInQualityGate,
      NEW_SECURITY_REMEDIATION_EFFORT_KEY, OPERATOR_GREATER_THAN, null, "5", LEAK_PERIOD);
    dbSession.commit();

    underTest.start();

    assertThat(db.countRowsOfTable("quality_gates")).isEqualTo(1);
    verifyCorrectBuiltInQualityGate();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate's conditions of [Sonar way] has been updated")).isTrue();
  }

  @Test
  public void upgrade_should_add_missing_condition() {
    insertMetrics();
    QualityGateDto builtInQualityGate = db.qualityGates().insertBuiltInQualityGate();
    List<QualityGateConditionDto> builtInConditions = createBuiltInConditions(builtInQualityGate);
    // Remove a condition
    QualityGateConditionDto conditionToBeDeleted = builtInConditions.get(new Random().nextInt(builtInConditions.size()));
    gateConditionDao.delete(conditionToBeDeleted, dbSession);
    dbSession.commit();

    underTest.start();

    assertThat(db.countRowsOfTable("quality_gates")).isEqualTo(1);
    verifyCorrectBuiltInQualityGate();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate's conditions of [Sonar way] has been updated")).isTrue();
  }

  @Test
  public void should_set_SonarWay_as_builtin_when_not_set() {
    insertMetrics();
    QualityGateDto qualityGate = dbClient.qualityGateDao().insert(dbSession, new QualityGateDto()
      .setName("Sonar way")
      .setUuid(Uuids.createFast())
      .setBuiltIn(false)
      .setCreatedAt(new Date()));
    dbSession.commit();
    createBuiltInConditions(qualityGate);
    dbSession.commit();

    underTest.start();

    assertThat(db.countRowsOfTable("quality_gates")).isEqualTo(1);
    verifyCorrectBuiltInQualityGate();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Quality gate [Sonar way] has been set as built-in")).isTrue();
  }

  @Test
  public void should_not_update_builtin_quality_gate_if_already_uptodate() {
    insertMetrics();
    QualityGateDto builtInQualityGate = db.qualityGates().insertBuiltInQualityGate();
    createBuiltInConditions(builtInQualityGate);
    dbSession.commit();

    underTest.start();

    assertThat(db.countRowsOfTable("quality_gates")).isEqualTo(1);
    verifyCorrectBuiltInQualityGate();
    // Log must not be present
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Quality gate [Sonar way] has been set as built-in")).isFalse();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate [Sonar way] has been created")).isFalse();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate's conditions of [Sonar way] has been updated")).isFalse();
  }

  @Test
  public void ensure_only_one_built_in_quality_gate() {
    insertMetrics();
    String qualityGateName = "IncorrectQualityGate";
    QualityGateDto builtin = new QualityGateDto().setName(qualityGateName).setBuiltIn(true).setUuid(Uuids.createFast());
    qualityGateDao.insert(dbSession, builtin);
    dbSession.commit();

    underTest.start();

    QualityGateDto oldQualityGate = qualityGateDao.selectByName(dbSession, qualityGateName);
    assertThat(oldQualityGate).isNotNull();
    assertThat(oldQualityGate.isBuiltIn()).isFalse();
    assertThat(db.select("select name as \"name\" from quality_gates where is_built_in is true"))
      .extracting(column -> column.get("name"))
      .containsExactly(BUILT_IN_NAME);
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate [Sonar way] has been created")).isTrue();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate's conditions of [Sonar way] has been updated")).isTrue();
  }

  @Test
  public void ensure_only_that_builtin_is_set_as_default_when_no_default_quality_gate() {
    insertMetrics();
    QualityGateDto builtInQualityGate = db.qualityGates().insertBuiltInQualityGate();

    underTest.start();

    assertThat(qualityGateFinder.getBuiltInQualityGate(dbSession)).isNotNull();
    assertThat(qualityGateFinder.getBuiltInQualityGate(dbSession).getId()).isEqualTo(builtInQualityGate.getId());
  }

  @Test
  public void builtin_quality_gate_with_incorrect_metricId_should_not_throw_an_exception() {
    insertMetrics();
    QualityGateConditionDto conditionDto = new QualityGateConditionDto()
      .setMetricId(-1) // This Id does not exist
      .setOperator(OPERATOR_GREATER_THAN)
      .setErrorThreshold("1")
      .setWarningThreshold("1");
    gateConditionDao.insert(conditionDto, dbSession);
    dbSession.commit();

    underTest.start();

    // No exception thrown
    verifyCorrectBuiltInQualityGate();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate's conditions of [Sonar way] has been updated")).isTrue();
  }

  private void insertMetrics() {
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey(NEW_RELIABILITY_RATING_KEY).setValueType(INT.name()).setHidden(false));
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey(NEW_SECURITY_RATING_KEY).setValueType(INT.name()).setHidden(false));
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey(NEW_SECURITY_REMEDIATION_EFFORT_KEY).setValueType(INT.name()).setHidden(false));
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey(NEW_MAINTAINABILITY_RATING_KEY).setValueType(PERCENT.name()).setHidden(false));
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey(NEW_COVERAGE_KEY).setValueType(PERCENT.name()).setHidden(false));
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey(NEW_DUPLICATED_LINES_DENSITY_KEY).setValueType(PERCENT.name()).setHidden(false));
    dbSession.commit();
  }

  private void verifyCorrectBuiltInQualityGate() {
    MetricDto newReliability = metricDao.selectByKey(dbSession, NEW_RELIABILITY_RATING_KEY);
    MetricDto newSecurity = metricDao.selectByKey(dbSession, NEW_SECURITY_RATING_KEY);
    MetricDto newMaintainability = metricDao.selectByKey(dbSession, NEW_MAINTAINABILITY_RATING_KEY);
    MetricDto newCoverage = metricDao.selectByKey(dbSession, NEW_COVERAGE_KEY);
    MetricDto newDuplication = metricDao.selectByKey(dbSession, NEW_DUPLICATED_LINES_DENSITY_KEY);

    QualityGateDto qualityGateDto = qualityGateDao.selectByName(dbSession, BUILT_IN_NAME);
    assertThat(qualityGateDto).isNotNull();
    assertThat(qualityGateDto.getCreatedAt()).isNotNull();
    assertThat(qualityGateDto.isBuiltIn()).isTrue();
    assertThat(gateConditionDao.selectForQualityGate(dbSession, qualityGateDto.getId()))
      .extracting(QualityGateConditionDto::getMetricId, QualityGateConditionDto::getOperator, QualityGateConditionDto::getWarningThreshold,
        QualityGateConditionDto::getErrorThreshold, QualityGateConditionDto::getPeriod)
      .containsOnly(
        tuple(newReliability.getId().longValue(), OPERATOR_GREATER_THAN, null, "1", 1),
        tuple(newSecurity.getId().longValue(), OPERATOR_GREATER_THAN, null, "1", 1),
        tuple(newMaintainability.getId().longValue(), OPERATOR_GREATER_THAN, null, "1", 1),
        tuple(newCoverage.getId().longValue(), OPERATOR_LESS_THAN, null, "80", 1),
        tuple(newDuplication.getId().longValue(), OPERATOR_GREATER_THAN, null, "3", 1));
  }

  private List<QualityGateConditionDto> createBuiltInConditions(QualityGateDto qg) {
    List<QualityGateConditionDto> conditions = new ArrayList<>();

    conditions.add(qualityGateConditionsUpdater.createCondition(dbSession, qg,
      NEW_SECURITY_RATING_KEY, OPERATOR_GREATER_THAN, null, "1", LEAK_PERIOD));
    conditions.add(qualityGateConditionsUpdater.createCondition(dbSession, qg,
      NEW_RELIABILITY_RATING_KEY, OPERATOR_GREATER_THAN, null, "1", LEAK_PERIOD));
    conditions.add(qualityGateConditionsUpdater.createCondition(dbSession, qg,
      NEW_MAINTAINABILITY_RATING_KEY, OPERATOR_GREATER_THAN, null, "1", LEAK_PERIOD));
    conditions.add(qualityGateConditionsUpdater.createCondition(dbSession, qg,
      NEW_COVERAGE_KEY, OPERATOR_LESS_THAN, null, "80", LEAK_PERIOD));
    conditions.add(qualityGateConditionsUpdater.createCondition(dbSession, qg,
      NEW_DUPLICATED_LINES_DENSITY_KEY, OPERATOR_GREATER_THAN, null, "3", LEAK_PERIOD));

    return conditions;
  }
}
