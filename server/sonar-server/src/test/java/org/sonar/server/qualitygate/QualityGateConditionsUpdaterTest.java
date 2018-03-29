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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;

import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.Metric.ValueType.BOOL;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.MILLISEC;
import static org.sonar.api.measures.Metric.ValueType.PERCENT;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.api.measures.Metric.ValueType.WORK_DUR;

@RunWith(DataProviderRunner.class)
public class QualityGateConditionsUpdaterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private QualityGateConditionsUpdater underTest = new QualityGateConditionsUpdater(db.getDbClient());

  @Test
  public void create_warning_condition_without_period() {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(PERCENT.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    QualityGateConditionDto result = underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "LT", "90", null, null);

    verifyCondition(result, qualityGate, metric, "LT", "90", null, null);
  }

  @Test
  public void create_error_condition_on_leak_period() {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey("new_coverage").setValueType(INT.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    QualityGateConditionDto result = underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "LT", null, "80", 1);

    verifyCondition(result, qualityGate, metric, "LT", null, "80", 1);
  }

  @Test
  public void fail_to_create_condition_when_condition_on_same_metric_already_exist() {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(PERCENT.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    db.qualityGates().addCondition(qualityGate, metric);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Condition on metric '%s' already exists.", metric.getShortName()));

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "LT", "90", null, null);
  }

  @Test
  public void fail_to_create_condition_when_condition_on_same_metric_and_on_leak_period_already_exist() {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(PERCENT.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    db.qualityGates().addCondition(qualityGate, metric, c -> c.setPeriod(1));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Condition on metric '%s' over leak period already exists.", metric.getShortName()));

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "LT", "90", null, 1);
  }

  @Test
  public void fail_to_create_condition_on_missing_metric() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("There is no metric with key=new_coverage");

    underTest.createCondition(db.getSession(), qualityGate, "new_coverage", "LT", null, "80", 2);
  }

  @Test
  @UseDataProvider("invalid_metrics")
  public void fail_to_create_condition_on_invalid_metric(String metricKey, Metric.ValueType valueType, boolean hidden) {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(metricKey).setValueType(valueType.name()).setHidden(hidden));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Metric '%s' cannot be used to define a condition.", metric.getKey()));

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "EQ", null, "80", null);
  }

  @Test
  public void fail_to_create_condition_on_not_allowed_operator() {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(PERCENT.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Operator UNKNOWN is not allowed for metric type PERCENT.");

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "UNKNOWN", null, "80", 2);
  }

  @Test
  public void fail_to_create_condition_on_missing_period() {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey("new_coverage").setValueType(INT.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("A period must be selected for differential metrics.");

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "EQ", null, "90", null);
  }

  @Test
  public void fail_to_create_condition_on_invalid_period() {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey("new_coverage").setValueType(INT.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The only valid quality gate period is 1, the leak period.");

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "EQ", null, "90", 6);
  }

  @Test
  public void create_condition_on_rating_metric() {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(SQALE_RATING_KEY).setValueType(RATING.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    QualityGateConditionDto result = underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "GT", null, "3", null);

    verifyCondition(result, qualityGate, metric, "GT", null, "3", null);
  }

  @Test
  public void fail_to_create_condition_on_rating_metric_on_leak_period() {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(SQALE_RATING_KEY).setValueType(RATING.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("The metric '%s' cannot be used on the leak period", metric.getShortName()));

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "GT", null, "3", 1);
  }

  @Test
  public void fail_to_create_warning_condition_on_invalid_rating_metric() {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(SQALE_RATING_KEY).setValueType(RATING.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("'6' is not a valid rating");

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "GT", "6", null, null);
  }

  @Test
  public void fail_to_create_error_condition_on_invalid_rating_metric() {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(SQALE_RATING_KEY).setValueType(RATING.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("'80' is not a valid rating");

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "GT", null, "80", null);
  }

  @Test
  public void fail_to_create_condition_on_greater_than_E() {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(SQALE_RATING_KEY).setValueType(RATING.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("There's no worse rating than E (5)");

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "GT", "5", null, null);
  }

  @Test
  @UseDataProvider("valid_values")
  public void create_warning_condition(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    QualityGateConditionDto result = underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "EQ", value, null, null);

    verifyCondition(result, qualityGate, metric, "EQ", value, null, null);
  }

  @Test
  @UseDataProvider("valid_values")
  public void create_error_condition(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    QualityGateConditionDto result = underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "EQ", null, value, null);

    verifyCondition(result, qualityGate, metric, "EQ", null, value, null);
  }

  @Test
  @UseDataProvider("invalid_values")
  public void fail_to_create_warning_INT_condition_when_value_is_not_an_integer(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Invalid value '%s' for metric '%s'", value, metric.getShortName()));

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "EQ", value, null, null);
  }

  @Test
  @UseDataProvider("invalid_values")
  public void fail_to_create_error_INT_condition_when_value_is_not_an_integer(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Invalid value '%s' for metric '%s'", value, metric.getShortName()));

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "EQ", null, value, null);
  }

  @Test
  public void update_condition() {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(PERCENT.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setWarningThreshold(null).setErrorThreshold("80").setPeriod(null));

    QualityGateConditionDto result = underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", "60", null, 1);

    verifyCondition(result, qualityGate, metric, "GT", "60", null, 1);
  }

  @Test
  public void update_condition_over_leak_period() {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(PERCENT.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("GT").setWarningThreshold("80").setErrorThreshold(null).setPeriod(1));

    QualityGateConditionDto result = underTest.updateCondition(db.getSession(), condition, metric.getKey(), "LT", null, "80", null);

    verifyCondition(result, qualityGate, metric, "LT", null, "80", null);
  }

  @Test
  public void update_condition_on_rating_metric() {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(SQALE_RATING_KEY).setValueType(RATING.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setWarningThreshold("80").setErrorThreshold(null).setPeriod(1));

    QualityGateConditionDto result = underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", "4", null, null);

    verifyCondition(result, qualityGate, metric, "GT", "4", null, null);
  }

  @Test
  public void fail_to_update_condition_on_rating_metric_on_leak_period() {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(SQALE_RATING_KEY).setValueType(RATING.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setWarningThreshold(null).setErrorThreshold("3").setPeriod(null));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("The metric '%s' cannot be used on the leak period", metric.getShortName()));

    underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", "4", null, 1);
  }

  @Test
  public void fail_to_update_condition_on_rating_metric_on_not_core_rating_metric() {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey("not_core_rating_metric").setValueType(RATING.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setWarningThreshold(null).setErrorThreshold("3").setPeriod(null));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("The metric '%s' cannot be used", metric.getShortName()));

    underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", "4", null, 1);
  }

  @Test
  @UseDataProvider("invalid_metrics")
  public void fail_to_update_condition_on_invalid_metric(String metricKey, Metric.ValueType valueType, boolean hidden) {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(metricKey).setValueType(valueType.name()).setHidden(hidden));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setWarningThreshold(null).setErrorThreshold("80").setPeriod(null));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Metric '%s' cannot be used to define a condition.", metric.getKey()));

    underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", "60", null, 1);
  }

  @Test
  public void fail_to_update_condition_when_condition_on_same_metric_already_exist() {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(INT.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto conditionNotOnLeakPeriod = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("GT").setWarningThreshold("80").setErrorThreshold(null).setPeriod(null));
    QualityGateConditionDto conditionOnLeakPeriod = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("GT").setWarningThreshold("80").setErrorThreshold(null).setPeriod(1));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Condition on metric '%s' over leak period already exists.", metric.getShortName()));

    // Update condition not on leak period to be on leak period => will fail as this condition already exist
    underTest.updateCondition(db.getSession(), conditionNotOnLeakPeriod, metric.getKey(), "GT", "80", null, 1);
  }

  @Test
  @UseDataProvider("valid_values")
  public void update_warning_condition(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setWarningThreshold(null).setErrorThreshold("80").setPeriod(null));

    QualityGateConditionDto result = underTest.updateCondition(db.getSession(), condition, metric.getKey(), "EQ", value, null, null);

    verifyCondition(result, qualityGate, metric, "EQ", value, null, null);
  }

  @Test
  @UseDataProvider("valid_values")
  public void update_error_condition(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setWarningThreshold(null).setErrorThreshold("80").setPeriod(null));

    QualityGateConditionDto result = underTest.updateCondition(db.getSession(), condition, metric.getKey(), "EQ", null, value, null);

    verifyCondition(result, qualityGate, metric, "EQ", null, value, null);
  }

  @Test
  @UseDataProvider("invalid_values")
  public void fail_to_update_warning_INT_condition_when_value_is_not_an_integer(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setWarningThreshold(null).setErrorThreshold("80").setPeriod(null));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Invalid value '%s' for metric '%s'", value, metric.getShortName()));

    underTest.updateCondition(db.getSession(), condition, metric.getKey(), "EQ", value, null, null);
  }

  @Test
  @UseDataProvider("invalid_values")
  public void fail_to_update_error_INT_condition_when_value_is_not_an_integer(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setWarningThreshold(null).setErrorThreshold("80").setPeriod(null));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Invalid value '%s' for metric '%s'", value, metric.getShortName()));

    underTest.updateCondition(db.getSession(), condition, metric.getKey(), "EQ", null, value, null);
  }

  @DataProvider
  public static Object[][] invalid_metrics() {
    return new Object[][] {
      {ALERT_STATUS_KEY, INT, false},
      {"data_metric", DATA, false},
      {"hidden", INT, true}
    };
  }

  @DataProvider
  public static Object[][] valid_values() {
    return new Object[][] {
      {INT, "10"},
      {BOOL, "1"},
      {MILLISEC, "1000"},
      {WORK_DUR, "1000"},
      {FLOAT, "5.12"},
      {PERCENT, "10.30"},
    };
  }

  @DataProvider
  public static Object[][] invalid_values() {
    return new Object[][] {
      {INT, "ABCD"},
      {BOOL, "ABCD"},
      {MILLISEC, "ABCD"},
      {WORK_DUR, "ABCD"},
      {FLOAT, "ABCD"},
      {PERCENT, "ABCD"},
    };
  }

  private void verifyCondition(QualityGateConditionDto dto, QualityGateDto qualityGate, MetricDto metric, String operator, @Nullable String warning, @Nullable String error,
    @Nullable Integer period) {
    QualityGateConditionDto reloaded = db.getDbClient().gateConditionDao().selectById(dto.getId(), db.getSession());
    assertThat(reloaded.getQualityGateId()).isEqualTo(qualityGate.getId());
    assertThat(reloaded.getMetricId()).isEqualTo(metric.getId().longValue());
    assertThat(reloaded.getOperator()).isEqualTo(operator);
    assertThat(reloaded.getWarningThreshold()).isEqualTo(warning);
    assertThat(reloaded.getErrorThreshold()).isEqualTo(error);
    assertThat(reloaded.getPeriod()).isEqualTo(period);

    assertThat(dto.getQualityGateId()).isEqualTo(qualityGate.getId());
    assertThat(dto.getMetricId()).isEqualTo(metric.getId().longValue());
    assertThat(dto.getOperator()).isEqualTo(operator);
    assertThat(dto.getWarningThreshold()).isEqualTo(warning);
    assertThat(dto.getErrorThreshold()).isEqualTo(error);
    assertThat(dto.getPeriod()).isEqualTo(period);
  }

}
