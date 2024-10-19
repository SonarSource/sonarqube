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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
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
public class QualityGateConditionsUpdaterIT {

  @Rule
  public DbTester db = DbTester.create();

  private final QualityGateConditionsUpdater underTest = new QualityGateConditionsUpdater(db.getDbClient());

  @Test
  public void create_error_condition() {
    MetricDto metric = insertMetric(INT, "new_coverage");
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    QualityGateConditionDto result = underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "LT", "80");

    verifyCondition(result, qualityGate, metric, "LT", "80");
  }

  @Test
  @UseDataProvider("valid_operators_and_direction")
  public void create_condition_with_valid_operators_and_direction(String operator, int direction) {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey("key").setValueType(INT.name()).setHidden(false).setDirection(direction));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    QualityGateConditionDto result = underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), operator, "80");

    verifyCondition(result, qualityGate, metric, operator, "80");
  }

  @Test
  public void create_condition_throws_NPE_if_errorThreshold_is_null() {
    MetricDto metric = insertMetric(RATING, SQALE_RATING_KEY);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    assertThatThrownBy(() -> underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "GT", null))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("errorThreshold can not be null");
  }

  @Test
  public void fail_to_create_condition_when_condition_on_same_metric_already_exist() {
    MetricDto metric = insertMetric(PERCENT);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().addCondition(qualityGate, metric);

    assertThatThrownBy(() -> underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "LT", "80"))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining(format("Condition on metric '%s' already exists.", metric.getShortName()));
  }

  @Test
  public void fail_to_create_condition_on_missing_metric() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    assertThatThrownBy(() -> underTest.createCondition(db.getSession(), qualityGate, "new_coverage", "LT", "80"))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("There is no metric with key=new_coverage");
  }

  @Test
  @UseDataProvider("invalid_metrics")
  public void fail_to_create_condition_on_invalid_metric(String metricKey, Metric.ValueType valueType, boolean hidden) {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(metricKey).setValueType(valueType.name()).setHidden(hidden).setDirection(0));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    assertThatThrownBy(() -> underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "LT", "80"))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining(format("Metric '%s' cannot be used to define a condition", metric.getKey()));
  }

  @Test
  @UseDataProvider("invalid_operators_and_direction")
  public void fail_to_create_condition_on_not_allowed_operator_for_metric_direction(String operator, int direction) {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey("key").setValueType(INT.name()).setHidden(false).setDirection(direction));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    assertThatThrownBy(() -> underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), operator, "90"))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining(format("Operator %s is not allowed for this metric.", operator));
  }

  @Test
  public void create_condition_on_rating_metric() {
    MetricDto metric = insertMetric(RATING, SQALE_RATING_KEY);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    QualityGateConditionDto result = underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "GT", "3");

    verifyCondition(result, qualityGate, metric, "GT", "3");
  }

  @Test
  public void fail_to_create_error_condition_on_invalid_rating_metric() {
    MetricDto metric = insertMetric(RATING, SQALE_RATING_KEY);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    assertThatThrownBy(() -> underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "GT", "80"))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("'80' is not a valid rating");
  }

  @Test
  public void fail_to_create_condition_on_rating_greater_than_E() {
    MetricDto metric = insertMetric(RATING, SQALE_RATING_KEY);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    assertThatThrownBy(() -> underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "GT", "5"))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("There's no worse rating than E (5)");
  }

  @Test
  @UseDataProvider("valid_values")
  public void create_error_condition(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false).setDirection(0));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    QualityGateConditionDto result = underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "LT", value);

    verifyCondition(result, qualityGate, metric, "LT", value);
  }

  @Test
  @UseDataProvider("invalid_values")
  public void fail_to_create_error_INT_condition_when_value_is_not_an_integer(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false).setDirection(0));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    assertThatThrownBy(() -> underTest.createCondition(db.getSession(), qualityGate, metric.getKey(), "LT", value))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining(format("Invalid value '%s' for metric '%s'", value, metric.getShortName()));
  }

  @Test
  public void update_condition() {
    MetricDto metric = insertMetric(PERCENT);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("80"));

    QualityGateConditionDto result = underTest.updateCondition(db.getSession(), condition, metric.getKey(), "LT", "80");

    verifyCondition(result, qualityGate, metric, "LT", "80");
  }

  @Test
  public void update_condition_throws_NPE_if_errorThreshold_is_null() {
    MetricDto metric = insertMetric(PERCENT);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("80"));

    assertThatThrownBy(() -> underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", null))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("errorThreshold can not be null");
  }

  @Test
  public void update_condition_on_rating_metric() {
    MetricDto metric = insertMetric(RATING, SQALE_RATING_KEY);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("80"));

    QualityGateConditionDto result = underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", "4");

    verifyCondition(result, qualityGate, metric, "GT", "4");
  }

  @Test
  @UseDataProvider("update_invalid_operators_and_direction")
  public void fail_to_update_condition_on_not_allowed_operator_for_metric_direction(String validOperator, String updatedOperator, int direction) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(PERCENT.name()).setHidden(false).setDirection(direction));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator(validOperator).setErrorThreshold("80"));

    assertThatThrownBy(() -> underTest.updateCondition(db.getSession(), condition, metric.getKey(), updatedOperator, "70"))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining(format("Operator %s is not allowed for this metric", updatedOperator));
  }

  @Test
  public void fail_to_update_condition_on_rating_metric_on_new_code_period() {
    MetricDto metric = insertMetric(RATING, SQALE_RATING_KEY);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("3"));

    QualityGateConditionDto result = underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", "4");

    verifyCondition(result, qualityGate, metric, "GT", "4");
  }

  @Test
  public void fail_to_update_condition_on_rating_metric_on_not_core_rating_metric() {
    MetricDto metric = insertMetric(RATING, "not_core_rating_metric");
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("3"));

    assertThatThrownBy(() -> underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", "4"))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining(format("The metric '%s' cannot be used", metric.getShortName()));
  }

  @Test
  @UseDataProvider("invalid_metrics")
  public void fail_to_update_condition_on_invalid_metric(String metricKey, Metric.ValueType valueType, boolean hidden) {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(metricKey).setValueType(valueType.name()).setHidden(hidden));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("80"));

    assertThatThrownBy(() -> underTest.updateCondition(db.getSession(), condition, metric.getKey(), "GT", "60"))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining(format("Metric '%s' cannot be used to define a condition", metric.getKey()));
  }

  @Test
  @UseDataProvider("valid_values")
  public void update_error_condition(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false).setDirection(0));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("80"));

    QualityGateConditionDto result = underTest.updateCondition(db.getSession(), condition, metric.getKey(), "LT", value);

    verifyCondition(result, qualityGate, metric, "LT", value);
  }

  @Test
  @UseDataProvider("invalid_values")
  public void fail_to_update_error_INT_condition_when_value_is_not_an_integer(Metric.ValueType valueType, String value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(valueType.name()).setHidden(false).setDirection(0));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("80"));

    assertThatThrownBy(() -> underTest.updateCondition(db.getSession(), condition, metric.getKey(), "LT", value))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining(format("Invalid value '%s' for metric '%s'", value, metric.getShortName()));
  }

  @DataProvider
  public static Object[][] invalid_metrics() {
    return new Object[][] {
      {ALERT_STATUS_KEY, INT, false},
      {SECURITY_HOTSPOTS_KEY, INT, false},
      {NEW_SECURITY_HOTSPOTS_KEY, INT, false},
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
      .setDirection(0));
  }

  private void verifyCondition(QualityGateConditionDto dto, QualityGateDto qualityGate, MetricDto metric, String operator, String error) {
    QualityGateConditionDto reloaded = db.getDbClient().gateConditionDao().selectByUuid(dto.getUuid(), db.getSession());
    assertThat(reloaded.getQualityGateUuid()).isEqualTo(qualityGate.getUuid());
    assertThat(reloaded.getMetricUuid()).isEqualTo(metric.getUuid());
    assertThat(reloaded.getOperator()).isEqualTo(operator);
    assertThat(reloaded.getErrorThreshold()).isEqualTo(error);

    assertThat(dto.getQualityGateUuid()).isEqualTo(qualityGate.getUuid());
    assertThat(dto.getMetricUuid()).isEqualTo(metric.getUuid());
    assertThat(dto.getOperator()).isEqualTo(operator);
    assertThat(dto.getErrorThreshold()).isEqualTo(error);
  }

}
