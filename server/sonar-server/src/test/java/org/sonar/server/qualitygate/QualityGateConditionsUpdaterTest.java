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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDbTester;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.server.computation.task.projectanalysis.metric.Metric.MetricType.PERCENT;

@RunWith(DataProviderRunner.class)
public class QualityGateConditionsUpdaterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();
  QualityGateDbTester qualityGateDbTester = new QualityGateDbTester(db);

  QualityGateDto qualityGateDto;
  MetricDto coverageMetricDto = newMetricDto()
    .setKey("coverage")
    .setShortName("Coverage")
    .setValueType(PERCENT.name())
    .setHidden(false);

  MetricDto ratingMetricDto = newMetricDto()
    .setKey("reliability_rating")
    .setShortName("Reliability Rating")
    .setValueType(RATING.name())
    .setHidden(false);

  QualityGateConditionsUpdater underTest = new QualityGateConditionsUpdater(dbClient);

  @Before
  public void setUp() throws Exception {
    qualityGateDto = qualityGateDbTester.insertQualityGate(db.getDefaultOrganization());
    dbClient.metricDao().insert(dbSession, coverageMetricDto, ratingMetricDto);
    dbSession.commit();
  }

  @Test
  public void create_warning_condition_without_period() {
    QualityGateConditionDto result = underTest.createCondition(dbSession, qualityGateDto, "coverage", "LT", "90", null, null);

    verifyCondition(result, coverageMetricDto.getId(), "LT", "90", null, null);
  }

  @Test
  public void create_error_condition_with_period() {
    MetricDto metricDto = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("new_coverage")
      .setValueType(INT.name())
      .setHidden(false));
    dbSession.commit();

    QualityGateConditionDto result = underTest.createCondition(dbSession, qualityGateDto, "new_coverage", "LT", null, "80", 1);

    verifyCondition(result, metricDto.getId(), "LT", null, "80", 1);
  }

  @Test
  public void fail_to_create_condition_when_condition_on_same_metric_already_exist() {
    dbClient.gateConditionDao().insert(new QualityGateConditionDto()
      .setQualityGateId(qualityGateDto.getId())
      .setMetricId(coverageMetricDto.getId())
      .setPeriod(null),
      dbSession);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Condition on metric 'Coverage' already exists.");
    underTest.createCondition(dbSession, qualityGateDto, coverageMetricDto.getKey(), "LT", "90", null, null);
  }

  @Test
  public void fail_to_create_condition_when_condition_on_same_metric_and_on_leak_period_already_exist() {
    dbClient.gateConditionDao().insert(new QualityGateConditionDto()
      .setQualityGateId(qualityGateDto.getId())
      .setMetricId(coverageMetricDto.getId())
      .setPeriod(1),
      dbSession);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Condition on metric 'Coverage' over leak period already exists.");
    underTest.createCondition(dbSession, qualityGateDto, coverageMetricDto.getKey(), "LT", "90", null, 1);
  }

  @Test
  public void fail_to_create_condition_on_missing_metric() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("There is no metric with key=new_coverage");
    underTest.createCondition(dbSession, qualityGateDto, "new_coverage", "LT", null, "80", 2);
  }

  @Test
  @UseDataProvider("invalid_metrics")
  public void fail_to_create_condition_on_invalid_metric(String metricKey, Metric.ValueType valueType, boolean hidden) {
    dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey(metricKey)
      .setValueType(valueType.name())
      .setHidden(hidden));
    dbSession.commit();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Metric '" + metricKey + "' cannot be used to define a condition.");
    underTest.createCondition(dbSession, qualityGateDto, metricKey, "EQ", null, "80", null);
  }

  @Test
  public void fail_to_create_condition_on_not_allowed_operator() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Operator UNKNOWN is not allowed for metric type PERCENT.");
    underTest.createCondition(dbSession, qualityGateDto, "coverage", "UNKNOWN", null, "80", 2);
  }

  @Test
  public void fail_to_create_condition_on_missing_period() {
    dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("new_coverage")
      .setValueType(INT.name())
      .setHidden(false));
    dbSession.commit();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("A period must be selected for differential metrics.");
    underTest.createCondition(dbSession, qualityGateDto, "new_coverage", "EQ", null, "90", null);
  }

  @Test
  public void fail_to_create_condition_on_invalid_period() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The only valid quality gate period is 1, the leak period.");
    underTest.createCondition(dbSession, qualityGateDto, "coverage", "EQ", null, "90", 6);
  }

  @Test
  public void create_condition_on_rating_metric() {
    QualityGateConditionDto result = underTest.createCondition(dbSession, qualityGateDto, ratingMetricDto.getKey(), "GT", null, "3", null);

    verifyCondition(result, ratingMetricDto.getId(), "GT", null, "3", null);
  }

  @Test
  public void fail_to_create_condition_on_rating_metric_on_leak_period() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The metric 'Reliability Rating' cannot be used on the leak period");
    underTest.createCondition(dbSession, qualityGateDto, ratingMetricDto.getKey(), "GT", null, "3", 1);
  }

  @Test
  public void fail_to_create_warning_condition_on_invalid_rating_metric() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("'6' is not a valid rating");
    underTest.createCondition(dbSession, qualityGateDto, ratingMetricDto.getKey(), "GT", "6", null, null);
  }

  @Test
  public void fail_to_create_error_condition_on_invalid_rating_metric() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("'80' is not a valid rating");
    underTest.createCondition(dbSession, qualityGateDto, ratingMetricDto.getKey(), "GT", null, "80", null);
  }

  @Test
  public void fail_to_create_condition_on_greater_than_E() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("There's no worse rating than E (5)");
    underTest.createCondition(dbSession, qualityGateDto, ratingMetricDto.getKey(), "GT", "5", null, null);
  }

  @Test
  public void update_condition() {
    QualityGateConditionDto condition = insertCondition(coverageMetricDto.getId(), "LT", null, "80", null);

    QualityGateConditionDto result = underTest.updateCondition(dbSession, condition, "coverage", "GT", "60", null, 1);

    verifyCondition(result, coverageMetricDto.getId(), "GT", "60", null, 1);
  }

  @Test
  public void update_condition_over_leak_period() {
    QualityGateConditionDto condition = insertCondition(coverageMetricDto.getId(), "GT", "80", null, 1);

    QualityGateConditionDto result = underTest.updateCondition(dbSession, condition, "coverage", "LT", null, "80", null);

    verifyCondition(result, coverageMetricDto.getId(), "LT", null, "80", null);
  }

  @Test
  public void update_condition_on_rating_metric() {
    QualityGateConditionDto condition = insertCondition(ratingMetricDto.getId(), "LT", null, "3", null);

    QualityGateConditionDto result = underTest.updateCondition(dbSession, condition, ratingMetricDto.getKey(), "GT", "4", null, null);

    verifyCondition(result, ratingMetricDto.getId(), "GT", "4", null, null);
  }

  @Test
  public void fail_to_update_condition_on_rating_metric_on_leak_period() {
    QualityGateConditionDto condition = insertCondition(ratingMetricDto.getId(), "LT", null, "3", null);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The metric 'Reliability Rating' cannot be used on the leak period");
    underTest.updateCondition(dbSession, condition, ratingMetricDto.getKey(), "GT", "4", null, 1);
  }

  @Test
  public void fail_to_update_condition_on_rating_metric_on_not_core_rating_metric() {
    MetricDto metricDto = dbClient.metricDao().insert(dbSession, newMetricDto().setKey("rating_metric")
      .setShortName("Not core rating")
      .setValueType(RATING.name()).setHidden(false));
    QualityGateConditionDto condition = insertCondition(metricDto.getId(), "LT", null, "3", null);
    dbSession.commit();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The metric 'Not core rating' cannot be used");
    underTest.updateCondition(dbSession, condition, metricDto.getKey(), "GT", "4", null, 1);
  }

  @Test
  @UseDataProvider("invalid_metrics")
  public void fail_to_update_condition_on_invalid_metric(String metricKey, Metric.ValueType valueType, boolean hidden) {
    MetricDto metricDto = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey(metricKey)
      .setValueType(valueType.name())
      .setHidden(hidden));
    QualityGateConditionDto condition = insertCondition(metricDto.getId(), "LT", null, "80", null);
    dbSession.commit();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Metric '" + metricKey + "' cannot be used to define a condition.");
    underTest.updateCondition(dbSession, condition, metricDto.getKey(), "GT", "60", null, 1);
  }

  @Test
  public void fail_to_update_condition_when_condition_on_same_metric_already_exist() {
    QualityGateConditionDto conditionNotOnLeakPeriod = insertCondition(coverageMetricDto.getId(), "GT", "80", null, null);
    QualityGateConditionDto conditionOnLeakPeriod = insertCondition(coverageMetricDto.getId(), "GT", "80", null, 1);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Condition on metric 'Coverage' over leak period already exists.");
    // Update condition not on leak period to be on leak period => will fail as this condition already exist
    underTest.updateCondition(dbSession, conditionNotOnLeakPeriod, coverageMetricDto.getKey(), "GT", "80", null, 1);
  }

  @DataProvider
  public static Object[][] invalid_metrics() {
    return new Object[][] {
      {ALERT_STATUS_KEY, INT, false},
      {"data_metric", DATA, false},
      {"hidden", INT, true}
    };
  }

  private QualityGateConditionDto insertCondition(long metricId, String operator, @Nullable String warning, @Nullable String error,
    @Nullable Integer period) {
    QualityGateConditionDto qualityGateConditionDto = new QualityGateConditionDto().setQualityGateId(qualityGateDto.getId())
      .setMetricId(metricId)
      .setOperator(operator)
      .setWarningThreshold(warning)
      .setErrorThreshold(error)
      .setPeriod(period);
    dbClient.gateConditionDao().insert(qualityGateConditionDto, dbSession);
    dbSession.commit();
    return qualityGateConditionDto;
  }

  private void verifyCondition(QualityGateConditionDto dto, int metricId, String operator, @Nullable String warning, @Nullable String error, @Nullable Integer period) {
    QualityGateConditionDto reloaded = dbClient.gateConditionDao().selectById(dto.getId(), dbSession);
    assertThat(reloaded.getQualityGateId()).isEqualTo(qualityGateDto.getId());
    assertThat(reloaded.getMetricId()).isEqualTo(metricId);
    assertThat(reloaded.getOperator()).isEqualTo(operator);
    assertThat(reloaded.getWarningThreshold()).isEqualTo(warning);
    assertThat(reloaded.getErrorThreshold()).isEqualTo(error);
    assertThat(reloaded.getPeriod()).isEqualTo(period);

    assertThat(dto.getQualityGateId()).isEqualTo(qualityGateDto.getId());
    assertThat(dto.getMetricId()).isEqualTo(metricId);
    assertThat(dto.getOperator()).isEqualTo(operator);
    assertThat(dto.getWarningThreshold()).isEqualTo(warning);
    assertThat(dto.getErrorThreshold()).isEqualTo(error);
    assertThat(dto.getPeriod()).isEqualTo(period);
  }

}
