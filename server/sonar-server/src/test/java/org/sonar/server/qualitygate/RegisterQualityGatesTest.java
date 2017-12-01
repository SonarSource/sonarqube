/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.List;
import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.Uuids;
import org.sonar.core.util.stream.MoreCollectors;
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
  private QualityGateUpdater qualityGateUpdater = new QualityGateUpdater(dbClient, UuidFactoryFast.getInstance());
  private QualityGateFinder qualityGateFinder = new QualityGateFinder(dbClient);

  private RegisterQualityGates underTest = new RegisterQualityGates(dbClient, qualityGateUpdater, qualityGateConditionsUpdater, qualityGateFinder,
    UuidFactoryFast.getInstance(), System2.INSTANCE);

  @Before
  public void setup() {
    insertMetrics();
  }

  @After
  public void after() {
    underTest.stop();
  }

  @Test
  public void register_default_gate() {
    underTest.start();

    verifyCorrectBuiltInQualityGate();

    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate [Sonar way] has been created")
    ).isTrue();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate's conditions of [Sonar way] has been updated")
    ).isTrue();
  }

  @Test
  public void upgrade_empty_quality_gate() {
    QualityGateDto builtin = new QualityGateDto().setName(BUILT_IN_NAME).setBuiltIn(true).setUuid(Uuids.createFast());
    qualityGateDao.insert(dbSession, builtin);
    dbSession.commit();

    underTest.start();
    assertThat(qualityGateDao.selectAll(dbSession)).hasSize(1);
    verifyCorrectBuiltInQualityGate();

    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate's conditions of [Sonar way] has been updated")
    ).isTrue();
  }

  @Test
  public void upgrade_should_remove_deleted_condition() {
    QualityGateDto builtin = new QualityGateDto().setName(BUILT_IN_NAME).setBuiltIn(true).setUuid(Uuids.createFast());
    qualityGateDao.insert(dbSession, builtin);

    createBuiltInConditions(builtin);

    // Add another condition
    qualityGateConditionsUpdater.createCondition(dbSession, builtin,
      NEW_SECURITY_REMEDIATION_EFFORT_KEY, OPERATOR_GREATER_THAN, null, "5", LEAK_PERIOD);

    dbSession.commit();

    underTest.start();
    assertThat(qualityGateDao.selectAll(dbSession)).hasSize(1);
    verifyCorrectBuiltInQualityGate();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate's conditions of [Sonar way] has been updated")
    ).isTrue();
  }

  @Test
  public void upgrade_should_add_missing_condition() {
    QualityGateDto builtin = new QualityGateDto().setName(BUILT_IN_NAME).setBuiltIn(true).setUuid(Uuids.createFast());
    qualityGateDao.insert(dbSession, builtin);

    List<QualityGateConditionDto> builtInConditions = createBuiltInConditions(builtin);

    // Remove a condition
    QualityGateConditionDto conditionToBeDeleted = builtInConditions.get(new Random().nextInt(builtInConditions.size()));
    gateConditionDao.delete(conditionToBeDeleted, dbSession);

    dbSession.commit();

    underTest.start();
    assertThat(qualityGateDao.selectAll(dbSession)).hasSize(1);
    verifyCorrectBuiltInQualityGate();

    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate's conditions of [Sonar way] has been updated")
    ).isTrue();
  }

  @Test
  public void should_set_SonarWay_as_builtin_when_not_set() {
    QualityGateDto builtin = new QualityGateDto().setName(BUILT_IN_NAME).setBuiltIn(false).setUuid(Uuids.createFast());
    qualityGateDao.insert(dbSession, builtin);

    createBuiltInConditions(builtin);
    dbSession.commit();

    underTest.start();
    assertThat(qualityGateDao.selectAll(dbSession)).hasSize(1);
    verifyCorrectBuiltInQualityGate();

    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Quality gate [Sonar way] has been set as built-in")
    ).isTrue();
  }

  @Test
  public void should_not_update_builtin_quality_gate_if_already_uptodate() {
    QualityGateDto builtin = new QualityGateDto().setName(BUILT_IN_NAME).setBuiltIn(true).setUuid(Uuids.createFast());
    qualityGateDao.insert(dbSession, builtin);

    createBuiltInConditions(builtin);
    dbSession.commit();

    underTest.start();
    assertThat(qualityGateDao.selectAll(dbSession)).hasSize(1);
    verifyCorrectBuiltInQualityGate();

    // Log must not be present
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Quality gate [Sonar way] has been set as built-in")
    ).isFalse();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate [Sonar way] has been created")
    ).isFalse();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate's conditions of [Sonar way] has been updated")
    ).isFalse();
  }

  @Test
  public void ensure_only_one_built_in_quality_gate() {
    String qualityGateName = "IncorrectQualityGate";
    QualityGateDto builtin = new QualityGateDto().setName(qualityGateName).setBuiltIn(true).setUuid(Uuids.createFast());
    qualityGateDao.insert(dbSession, builtin);
    dbSession.commit();

    underTest.start();

    QualityGateDto oldQualityGate = qualityGateDao.selectByName(dbSession, qualityGateName);
    assertThat(oldQualityGate).isNotNull();
    assertThat(oldQualityGate.isBuiltIn()).isFalse();

    List<QualityGateDto> allBuiltInQualityProfiles = qualityGateDao.selectAll(dbSession)
      .stream()
      .filter(QualityGateDto::isBuiltIn)
      .collect(MoreCollectors.toList());
    assertThat(allBuiltInQualityProfiles)
      .extracting("name")
      .containsExactly(BUILT_IN_NAME);

    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate [Sonar way] has been created")
    ).isTrue();
    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate's conditions of [Sonar way] has been updated")
    ).isTrue();
  }

  @Test
  public void ensure_only_that_builtin_is_set_as_default_when_no_default_quality_gate() {
    QualityGateDto builtin = new QualityGateDto().setName(BUILT_IN_NAME).setBuiltIn(true).setUuid(Uuids.createFast());
    qualityGateDao.insert(dbSession, builtin);
    dbSession.commit();

    underTest.start();

    assertThat(qualityGateFinder.getDefault(dbSession)).isPresent();
    assertThat(qualityGateFinder.getDefault(dbSession).get().getId()).isEqualTo(builtin.getId());

    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate [Sonar way] has been set as default")
    ).isTrue();
  }

  @Test
  public void builtin_quality_gate_with_incorrect_metricId_should_not_throw_an_exception() {
    QualityGateDto builtin = new QualityGateDto().setName(BUILT_IN_NAME).setBuiltIn(true).setUuid(Uuids.createFast());
    qualityGateDao.insert(dbSession, builtin);
    QualityGateConditionDto conditionDto = new QualityGateConditionDto()
      .setMetricId(-1) // This Id does not exist
      .setOperator(OPERATOR_GREATER_THAN)
      .setErrorThreshold("1")
      .setWarningThreshold("1");
    gateConditionDao.insert(conditionDto,dbSession);
    dbSession.commit();

    underTest.start();

    // No exception thrown
    verifyCorrectBuiltInQualityGate();

    assertThat(
      logTester.logs(LoggerLevel.INFO).contains("Built-in quality gate's conditions of [Sonar way] has been updated")
    ).isTrue();
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

  private List<QualityGateConditionDto> createBuiltInConditions(QualityGateDto builtin) {
    List<QualityGateConditionDto> conditions = new ArrayList<>();

    conditions.add(qualityGateConditionsUpdater.createCondition(dbSession, builtin,
      NEW_SECURITY_RATING_KEY, OPERATOR_GREATER_THAN, null, "1", LEAK_PERIOD));
    conditions.add(qualityGateConditionsUpdater.createCondition(dbSession, builtin,
      NEW_RELIABILITY_RATING_KEY, OPERATOR_GREATER_THAN, null, "1", LEAK_PERIOD));
    conditions.add(qualityGateConditionsUpdater.createCondition(dbSession, builtin,
      NEW_MAINTAINABILITY_RATING_KEY, OPERATOR_GREATER_THAN, null, "1", LEAK_PERIOD));
    conditions.add(qualityGateConditionsUpdater.createCondition(dbSession, builtin,
      NEW_COVERAGE_KEY, OPERATOR_LESS_THAN, null, "80", LEAK_PERIOD));
    conditions.add(qualityGateConditionsUpdater.createCondition(dbSession, builtin,
      NEW_DUPLICATED_LINES_DENSITY_KEY, OPERATOR_GREATER_THAN, null, "3", LEAK_PERIOD));

    return conditions;
  }
}
