/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
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
import org.sonar.server.qualitygate.builtin.BuiltInQualityGate;
import org.sonar.server.qualitygate.builtin.SonarWayQualityGate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_VIOLATIONS_KEY;
import static org.mockito.Mockito.mock;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.PERCENT;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_GREATER_THAN;
import static org.sonar.db.qualitygate.QualityGateConditionDto.OPERATOR_LESS_THAN;

@RunWith(DataProviderRunner.class)
public class RegisterQualityGatesIT {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public LogTester logTester = new LogTester();
  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();

  private final QualityGateDao qualityGateDao = dbClient.qualityGateDao();
  private final QualityGateConditionDao gateConditionDao = dbClient.gateConditionDao();
  private final MetricDao metricDao = dbClient.metricDao();

  private final ValidQualityGateRatingMetricKeysProvider validQualityGateRatingMetricKeysProvider = mock(ValidQualityGateRatingMetricKeysProvider.class);

  private final QualityGateConditionsUpdater qualityGateConditionsUpdater = new QualityGateConditionsUpdater(dbClient, validQualityGateRatingMetricKeysProvider);

  private final RegisterQualityGates underTest = new RegisterQualityGates(dbClient,
    new BuiltInQualityGate[] {new SonarWayQualityGate()},
    qualityGateConditionsUpdater,
    new SequenceUuidFactory(),
    System2.INSTANCE);

  @Test
  public void register_default_gate() {
    insertMetrics();

    underTest.start();

    verifyCorrectBuiltInQualityGate();
    assertThat(
      logTester.logs(Level.INFO)).contains("Quality Gate [Sonar way] has been created");
    assertThat(
      logTester.logs(Level.INFO)).contains("Quality Gate's conditions of [Sonar way] has been updated");
  }

  @Test
  public void upgrade_empty_quality_gate() {
    insertMetrics();

    underTest.start();

    assertThat(db.countRowsOfTable("quality_gates")).isOne();
    verifyCorrectBuiltInQualityGate();
    assertThat(
      logTester.logs(Level.INFO)).contains("Quality Gate's conditions of [Sonar way] has been updated");
  }

  @Test
  public void upgrade_should_remove_deleted_condition() {
    insertMetrics();
    QualityGateDto builtInQualityGate = db.qualityGates().insertBuiltInQualityGate();
    createBuiltInConditions(builtInQualityGate);

    // Add old conditions
    List.of(NEW_RELIABILITY_RATING_KEY, NEW_MAINTAINABILITY_RATING_KEY, NEW_SECURITY_RATING_KEY)
      .forEach(metricKey -> qualityGateConditionsUpdater.createCondition(dbSession, builtInQualityGate,
        metricKey, OPERATOR_GREATER_THAN, "1"));
    dbSession.commit();

    underTest.start();

    verifyCorrectBuiltInQualityGate();
    assertThat(
      logTester.logs(Level.INFO)).contains("Quality Gate's conditions of [Sonar way] has been updated");
  }

  @Test
  public void upgrade_should_remove_duplicated_conditions_if_found() {
    insertMetrics();
    QualityGateDto builtInQualityGate = db.qualityGates().insertBuiltInQualityGate();
    createBuiltInConditions(builtInQualityGate);
    // Conditions added twice as found in some DB instances
    createBuiltInConditionsWithoutCheckingDuplicates(builtInQualityGate);
    dbSession.commit();

    underTest.start();

    verifyCorrectBuiltInQualityGate();
    assertThat(
      logTester.logs(Level.INFO)).contains("Quality Gate's conditions of [Sonar way] has been updated");
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

    verifyCorrectBuiltInQualityGate();
    assertThat(
      logTester.logs(Level.INFO)).contains("Quality Gate's conditions of [Sonar way] has been updated");
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

    verifyCorrectBuiltInQualityGate();
    assertThat(
      logTester.logs(Level.INFO)).contains("Quality Gate [Sonar way] builtin flag has been updated to [true]");
  }

  @Test
  public void should_not_update_builtin_quality_gate_if_already_uptodate() {
    insertMetrics();
    QualityGateDto builtInQualityGate = db.qualityGates().insertBuiltInQualityGate();
    createBuiltInConditions(builtInQualityGate);
    dbSession.commit();

    underTest.start();

    verifyCorrectBuiltInQualityGate();
    // Log must not be present
    assertThat(
      logTester.logs(Level.INFO)).doesNotContain("Quality gate [Sonar way] has been set as built-in");
    assertThat(
      logTester.logs(Level.INFO)).doesNotContain("Built-in quality gate [Sonar way] has been created");
    assertThat(
      logTester.logs(Level.INFO)).doesNotContain("Built-in quality gate's conditions of [Sonar way] has been updated");
  }

  @Test
  public void ensure_only_one_built_in_quality_gate() {
    insertMetrics();
    String qualityGateName = "IncorrectQualityGate";
    QualityGateDto builtin = new QualityGateDto().setName(qualityGateName).setBuiltIn(true).setUuid(Uuids.createFast());
    qualityGateDao.insert(dbSession, builtin);
    dbSession.commit();

    underTest.start();

    QualityGateDto oldQualityGate = qualityGateDao.selectByUuid(dbSession, builtin.getUuid());
    assertThat(oldQualityGate).isNotNull();
    assertThat(oldQualityGate.getName()).startsWith("IncorrectQualityGate (backup from");
    assertThat(oldQualityGate.isBuiltIn()).isFalse();
    var qualityGateDto = qualityGateDao.selectByName(dbSession, SonarWayQualityGate.NAME);
    assertThat(qualityGateDto).isNotNull();
    assertThat(
      logTester.logs(Level.INFO)).contains("Quality Gate [Sonar way] has been created");
    assertThat(
      logTester.logs(Level.INFO)).contains("Quality Gate's conditions of [Sonar way] has been updated");
  }

  @Test
  public void builtin_quality_gate_with_incorrect_metricuuid_should_not_throw_an_exception() {
    insertMetrics();
    QualityGateConditionDto conditionDto = new QualityGateConditionDto()
      .setUuid(Uuids.createFast())
      .setQualityGateUuid("qgate_uuid")
      .setMetricUuid("unknown") // This uuid does not exist
      .setOperator(OPERATOR_GREATER_THAN)
      .setErrorThreshold("1");
    gateConditionDao.insert(conditionDto, dbSession);
    dbSession.commit();

    underTest.start();

    // No exception thrown
    verifyCorrectBuiltInQualityGate();
    assertThat(
      logTester.logs(Level.INFO)).contains("Quality Gate's conditions of [Sonar way] has been updated");
  }

  @DataProvider
  public static Object[][] data() {
    return new Object[][] {
      {false, true},
      {true, true},
      {true, false}
    };
  }

  private void insertMetrics() {
    dbClient.metricDao().insert(dbSession,
      newMetricDto().setKey(NEW_RELIABILITY_RATING_KEY).setValueType(INT.name()).setHidden(false).setDirection(0));
    dbClient.metricDao().insert(dbSession,
      newMetricDto().setKey(NEW_SECURITY_RATING_KEY).setValueType(INT.name()).setHidden(false).setDirection(0));
    dbClient.metricDao().insert(dbSession,
      newMetricDto().setKey(NEW_MAINTAINABILITY_RATING_KEY).setValueType(PERCENT.name()).setHidden(false).setDirection(0));
    dbClient.metricDao().insert(dbSession,
      newMetricDto().setKey(NEW_VIOLATIONS_KEY).setValueType(INT.name()).setHidden(false).setDirection(0));
    dbClient.metricDao().insert(dbSession,
      newMetricDto().setKey(NEW_COVERAGE_KEY).setValueType(PERCENT.name()).setHidden(false).setDirection(0));
    dbClient.metricDao().insert(dbSession,
      newMetricDto().setKey(NEW_DUPLICATED_LINES_DENSITY_KEY).setValueType(PERCENT.name()).setHidden(false).setDirection(0));
    dbClient.metricDao().insert(dbSession,
      newMetricDto().setKey(NEW_SECURITY_HOTSPOTS_REVIEWED_KEY).setValueType(PERCENT.name()).setHidden(false).setDirection(0));
    dbSession.commit();
  }

  private void verifyCorrectBuiltInQualityGate() {
    MetricDto newViolations = metricDao.selectByKey(dbSession, NEW_VIOLATIONS_KEY);
    MetricDto newCoverage = metricDao.selectByKey(dbSession, NEW_COVERAGE_KEY);
    MetricDto newDuplication = metricDao.selectByKey(dbSession, NEW_DUPLICATED_LINES_DENSITY_KEY);
    MetricDto newSecurityHotspots = metricDao.selectByKey(dbSession, NEW_SECURITY_HOTSPOTS_REVIEWED_KEY);

    QualityGateDto qualityGateDto = qualityGateDao.selectByName(dbSession, SonarWayQualityGate.NAME);
    assertThat(qualityGateDto).isNotNull();
    assertThat(qualityGateDto.getCreatedAt()).isNotNull();
    assertThat(qualityGateDto.isBuiltIn()).isTrue();
    assertThat(gateConditionDao.selectForQualityGate(dbSession, qualityGateDto.getUuid()))
      .extracting(QualityGateConditionDto::getMetricUuid, QualityGateConditionDto::getOperator,
        QualityGateConditionDto::getErrorThreshold)
      .containsExactlyInAnyOrder(
        tuple(newViolations.getUuid(), OPERATOR_GREATER_THAN, "0"),
        tuple(newCoverage.getUuid(), OPERATOR_LESS_THAN, "80"),
        tuple(newDuplication.getUuid(), OPERATOR_GREATER_THAN, "3"),
        tuple(newSecurityHotspots.getUuid(), OPERATOR_LESS_THAN, "100"));
  }

  private List<QualityGateConditionDto> createBuiltInConditions(QualityGateDto qg) {
    List<QualityGateConditionDto> conditions = new ArrayList<>();

    conditions.add(qualityGateConditionsUpdater.createCondition(dbSession, qg,
      NEW_VIOLATIONS_KEY, OPERATOR_GREATER_THAN, "0"));
    conditions.add(qualityGateConditionsUpdater.createCondition(dbSession, qg,
      NEW_COVERAGE_KEY, OPERATOR_LESS_THAN, "80"));
    conditions.add(qualityGateConditionsUpdater.createCondition(dbSession, qg,
      NEW_DUPLICATED_LINES_DENSITY_KEY, OPERATOR_GREATER_THAN, "3"));
    conditions.add(qualityGateConditionsUpdater.createCondition(dbSession, qg,
      NEW_SECURITY_HOTSPOTS_REVIEWED_KEY, OPERATOR_LESS_THAN, "100"));

    return conditions;
  }

  private List<QualityGateConditionDto> createBuiltInConditionsWithoutCheckingDuplicates(QualityGateDto qg) {
    List<QualityGateConditionDto> conditions = new ArrayList<>();

    conditions.add(createConditionWithoutCheckingDuplicates(qg,
      NEW_VIOLATIONS_KEY, OPERATOR_GREATER_THAN, "0"));
    conditions.add(createConditionWithoutCheckingDuplicates(qg,
      NEW_COVERAGE_KEY, OPERATOR_LESS_THAN, "80"));
    conditions.add(createConditionWithoutCheckingDuplicates(qg,
      NEW_DUPLICATED_LINES_DENSITY_KEY, OPERATOR_GREATER_THAN, "3"));
    conditions.add(createConditionWithoutCheckingDuplicates(qg,
      NEW_SECURITY_HOTSPOTS_REVIEWED_KEY, OPERATOR_LESS_THAN, "100"));

    return conditions;
  }

  private QualityGateConditionDto createConditionWithoutCheckingDuplicates(QualityGateDto qualityGate, String metricKey, String operator,
    String errorThreshold) {
    MetricDto metric = metricDao.selectByKey(dbSession, metricKey);

    QualityGateConditionDto newCondition = new QualityGateConditionDto().setQualityGateUuid(qualityGate.getUuid())
      .setUuid(Uuids.create())
      .setMetricUuid(metric.getUuid()).setMetricKey(metric.getKey())
      .setOperator(operator)
      .setErrorThreshold(errorThreshold);
    gateConditionDao.insert(newCondition, dbSession);
    return newCondition;
  }

}
