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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
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
import static java.lang.String.valueOf;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.Metric.ValueType.BOOL;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.MILLISEC;
import static org.sonar.api.measures.Metric.ValueType.PERCENT;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.api.measures.Metric.ValueType.WORK_DUR;

@RunWith(DataProviderRunner.class)
public class QualityGateConditionsUpdaterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private QualityGateConditionsUpdater underTest = new QualityGateConditionsUpdater(db.getDbClient());

  @Test
  public void create_error_condition() {
    MetricDto metric = insertMetric(INT, "new_coverage");
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    QualityGateConditionDto result = underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "LT", "80");

    verifyCondition(result, qualityGate, metric, "LT", "80");
  }

  @Test
  @UseDataProvider("valid_operators_and_direction")
  public void create_condition_with_valid_operators_and_direction(String operator, int direction) {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey("key").setValueType(INT.name()).setHidden(false).setDirection(direction));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    QualityGateConditionDto result = underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), operator, "80");

    verifyCondition(result, qualityGate, metric, operator, "80");
  }

  @Test
  public void create_condition_throws_NPE_if_errorThreshold_is_null() {
    MetricDto metric = insertMetric(RATING, SQALE_RATING_KEY);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("errorThreshold can not be null");

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "GT", null);
  }

  @Test
  public void fail_to_create_condition_when_condition_on_same_metric_already_exist() {
    MetricDto metric = insertMetric(PERCENT);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    db.qualityGates().addCondition(qualityGate, metric);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Condition on metric '%s' already exists.", metric.getShortName()));

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "LT", "80");
  }

  @Test
  public void fail_to_create_condition_on_missing_metric() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("There is no metric with key=new_coverage");

    underTest.createCondition(db.getSession(), qualityGate, "new_coverage", "LT", "80");
  }

  @Test
  @UseDataProvider("invalid_metrics")
  public void fail_to_create_condition_on_invalid_metric(String metricKey, Metric.ValueType valueType, boolean hidden) {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(metricKey).setValueType(valueType.name()).setHidden(hidden).setDirection(0));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Metric '%s' cannot be used to define a condition", metric.getKey()));

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "LT", "80");
  }

  @Test
  @UseDataProvider("invalid_operators_and_direction")
  public void fail_to_create_condition_on_not_allowed_operator_for_metric_direction(String operator, int direction) {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey("key").setValueType(INT.name()).setHidden(false).setDirection(direction));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Operator %s is not allowed for this metric.", operator));

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), operator, "90");
  }

  @Test
  public void create_condition_on_rating_metric() {
    MetricDto metric = insertMetric(RATING, SQALE_RATING_KEY);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    QualityGateConditionDto result = underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "GT", "3");

    verifyCondition(result, qualityGate, metric, "GT", "3");
  }

  @Test
  public void fail_to_create_error_condition_on_invalid_rating_metric() {
    MetricDto metric = insertMetric(RATING, SQALE_RATING_KEY);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("'80' is not a valid rating");

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "GT", "80");
  }

  @Test
  public void fail_to_create_condition_on_greater_than_E() {
    MetricDto metric = insertMetric(RATING, SQALE_RATING_KEY);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("There's no worse rating than E (5)");

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "GT", "5");
  }

  @Test
  @UseDataProvider("valid_values")
  public void create_error_condition(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false).setDirection(0));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    QualityGateConditionDto result = underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "LT", value);

    verifyCondition(result, qualityGate, metric, "LT", value);
  }

  @Test
  @UseDataProvider("invalid_values")
  public void fail_to_create_error_INT_condition_when_value_is_not_an_integer(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false).setDirection(0));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Invalid value '%s' for metric '%s'", value, metric.getShortName()));

    underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "LT", value);
  }

  @Test
  public void update_condition() {
    MetricDto metric = insertMetric(PERCENT);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("80"));

    QualityGateConditionDto result = underTest.updateCondition(db.getSession(), condition, metric.getKey(), "LT", "80");

    verifyCondition(result, qualityGate, metric, "LT", "80");
  }

  @Test
  public void update_condition_throws_NPE_if_errorThreshold_is_null() {
    MetricDto metric = insertMetric(PERCENT);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("80"));

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("errorThreshold can not be null");

    underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", null);
  }

  @Test
  public void update_condition_on_rating_metric() {
    MetricDto metric = insertMetric(RATING, SQALE_RATING_KEY);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("80"));

    QualityGateConditionDto result = underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", "4");

    verifyCondition(result, qualityGate, metric, "GT", "4");
  }

  @Test
  @UseDataProvider("update_invalid_operators_and_direction")
  public void fail_to_update_condition_on_not_allowed_operator_for_metric_direction(String validOperator, String updatedOperator, int direction) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(PERCENT.name()).setHidden(false).setDirection(direction));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator(validOperator).setErrorThreshold("80"));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Operator %s is not allowed for this metric", updatedOperator));

    underTest.updateCondition(db.getSession(), condition, metric.getKey(), updatedOperator, "70");
  }

  @Test
  public void fail_to_update_condition_on_rating_metric_on_leak_period() {
    MetricDto metric = insertMetric(RATING, SQALE_RATING_KEY);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("3"));

    QualityGateConditionDto result = underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", "4");

    verifyCondition(result, qualityGate, metric, "GT", "4");
  }

  @Test
  public void fail_to_update_condition_on_rating_metric_on_not_core_rating_metric() {
    MetricDto metric = insertMetric(RATING, "not_core_rating_metric");
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("3"));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("The metric '%s' cannot be used", metric.getShortName()));

    underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", "4");
  }

  @Test
  @UseDataProvider("invalid_metrics")
  public void fail_to_update_condition_on_invalid_metric(String metricKey, Metric.ValueType valueType, boolean hidden) {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(metricKey).setValueType(valueType.name()).setHidden(hidden));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("80"));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Metric '%s' cannot be used to define a condition", metric.getKey()));

    underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", "60");
  }

  @Test
  @UseDataProvider("valid_values")
  public void update_error_condition(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false).setDirection(0));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("80"));

    QualityGateConditionDto result = underTest.updateCondition(db.getSession(), condition, metric.getKey(), "LT", value);

    verifyCondition(result, qualityGate, metric, "LT", value);
  }

  @Test
  @UseDataProvider("invalid_values")
  public void fail_to_update_error_INT_condition_when_value_is_not_an_integer(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false).setDirection(0));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("80"));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Invalid value '%s' for metric '%s'", value, metric.getShortName()));

    underTest.updateCondition(db.getSession(), condition, metric.getKey(), "LT", value);
  }

  @DataProvider
  public static Object[][] invalid_metrics() {
    return new Object[][] {
      {ALERT_STATUS_KEY, INT, false},
      {"boolean", BOOL, false},
      {"string", STRING, false},
      {"data_metric", DATA, false},
      {"distrib", DISTRIB, false},
      {"hidden", INT, true}
    };
  }

  @DataProvider
  public static Object[][] valid_values() {
    return new Object[][] {
      {INT, "10"},
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
      {MILLISEC, "ABCD"},
      {WORK_DUR, "ABCD"},
      {FLOAT, "ABCD"},
      {PERCENT, "ABCD"},
    };
  }

  @DataProvider
  public static Object[][] invalid_operators_and_direction() {
    return new Object[][] {
      {"EQ", 0},
      {"NE", 0},
      {"LT", -1},
      {"GT", 1},
    };
  }

  @DataProvider
  public static Object[][] update_invalid_operators_and_direction() {
    return new Object[][] {
      {"LT", "EQ", 0},
      {"LT", "NE", 0},
      {"GT", "LT", -1},
      {"LT", "GT", 1},
    };
  }

  @DataProvider
  public static Object[][] valid_operators_and_direction() {
    return new Object[][] {
      {"LT", 0},
      {"GT", 0},
      {"GT", -1},
      {"LT", 1},
    };
  }

  private MetricDto insertMetric(Metric.ValueType type) {
    return insertMetric(type, "key");
  }

  private MetricDto insertMetric(Metric.ValueType type, String key) {
    return db.measures().insertMetric(m -> m
      .setKey(key)
      .setValueType(type.name())
      .setHidden(false)
      .setDirection(0)
    );
  }

  private void verifyCondition(QualityGateConditionDto dto, QualityGateDto qualityGate, MetricDto metric, String operator, String error) {
    QualityGateConditionDto reloaded = db.getDbClient().gateConditionDao().selectById(dto.getId(), db.getSession());
    assertThat(reloaded.getQualityGateId()).isEqualTo(qualityGate.getId());
    assertThat(reloaded.getMetricId()).isEqualTo(metric.getId().longValue());
    assertThat(reloaded.getOperator()).isEqualTo(operator);
    assertThat(reloaded.getErrorThreshold()).isEqualTo(error);

    assertThat(dto.getQualityGateId()).isEqualTo(qualityGate.getId());
    assertThat(dto.getMetricId()).isEqualTo(metric.getId().longValue());
    assertThat(dto.getOperator()).isEqualTo(operator);
    assertThat(dto.getErrorThreshold()).isEqualTo(error);
  }

}
