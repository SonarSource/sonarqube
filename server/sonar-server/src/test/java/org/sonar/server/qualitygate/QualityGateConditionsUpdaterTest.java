/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

  QualityGateConditionsUpdater underTest = new QualityGateConditionsUpdater(dbClient);

  @Before
  public void setUp() throws Exception {
    qualityGateDto = qualityGateDbTester.insertQualityGate();

    dbClient.metricDao().insert(dbSession, coverageMetricDto);
    dbSession.commit();
  }

  @Test
  public void create_warning_condition_without_period() {
    QualityGateConditionDto result = underTest.createCondition(dbSession, qualityGateDto.getId(), "coverage", "LT", "90", null, null);

    assertThat(result.getQualityGateId()).isEqualTo(qualityGateDto.getId());
    assertThat(result.getMetricId()).isEqualTo(coverageMetricDto.getId().longValue());
    assertThat(result.getOperator()).isEqualTo("LT");
    assertThat(result.getWarningThreshold()).isEqualTo("90");
    assertThat(result.getErrorThreshold()).isNull();
    assertThat(result.getPeriod()).isNull();
    assertThat(dbClient.gateConditionDao().selectById(result.getId(), dbSession)).isNotNull();
  }

  @Test
  public void create_error_condition_with_period() {
    MetricDto metricDto = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("new_coverage")
      .setValueType(INT.name())
      .setHidden(false));
    dbSession.commit();

    QualityGateConditionDto result = underTest.createCondition(dbSession, qualityGateDto.getId(), "new_coverage", "LT", null, "80", 1);

    assertThat(result.getQualityGateId()).isEqualTo(qualityGateDto.getId());
    assertThat(result.getMetricId()).isEqualTo(metricDto.getId().longValue());
    assertThat(result.getMetricKey()).isEqualTo("new_coverage");
    assertThat(result.getOperator()).isEqualTo("LT");
    assertThat(result.getWarningThreshold()).isNull();
    assertThat(result.getErrorThreshold()).isEqualTo("80");
    assertThat(result.getPeriod()).isEqualTo(1);
    assertThat(dbClient.gateConditionDao().selectById(result.getId(), dbSession)).isNotNull();
  }

  @Test
  public void fail_to_create_condition_when_condition_on_same_metric_already_exist() throws Exception {
    dbClient.gateConditionDao().insert(new QualityGateConditionDto()
      .setQualityGateId(qualityGateDto.getId())
      .setMetricId(coverageMetricDto.getId())
      .setPeriod(null),
      dbSession);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Condition on metric 'Coverage' already exists.");
    underTest.createCondition(dbSession, qualityGateDto.getId(), coverageMetricDto.getKey(), "LT", "90", null, null);
  }

  @Test
  public void fail_to_create_condition_when_condition_on_same_metric_and_on_leak_period_already_exist() throws Exception {
    dbClient.gateConditionDao().insert(new QualityGateConditionDto()
        .setQualityGateId(qualityGateDto.getId())
        .setMetricId(coverageMetricDto.getId())
        .setPeriod(1),
      dbSession);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Condition on metric 'Coverage' over leak period already exists.");
    underTest.createCondition(dbSession, qualityGateDto.getId(), coverageMetricDto.getKey(), "LT", "90", null, 1);
  }

  @Test
  public void fail_to_create_condition_on_missing_metric() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("There is no metric with key=new_coverage");
    underTest.createCondition(dbSession, qualityGateDto.getId(), "new_coverage", "LT", null, "80", 2);
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
    underTest.createCondition(dbSession, qualityGateDto.getId(), metricKey, "EQ", null, "80", null);
  }

  @Test
  public void fail_to_create_condition_on_not_allowed_operator() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Operator UNKNOWN is not allowed for metric type PERCENT.");
    underTest.createCondition(dbSession, qualityGateDto.getId(), "coverage", "UNKNOWN", null, "80", 2);
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
    underTest.createCondition(dbSession, qualityGateDto.getId(), "new_coverage", "EQ", null, "90", null);
  }

  @Test
  public void fail_to_create_condition_on_invalid_period() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The only valid quality gate period is 1, the leak period.");
    underTest.createCondition(dbSession, qualityGateDto.getId(), "coverage", "EQ", null, "90", 6);
  }

  @DataProvider
  public static Object[][] invalid_metrics() {
    return new Object[][] {
      {ALERT_STATUS_KEY, INT, false},
      {"data_metric", DATA, false},
      {"hidden", INT, true},
      {"rating_metric", RATING, false},
    };
  }

}
