/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.batch.qualitygate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.Alert;

public class AlertUtilsTest {
  private Metric metric;
  private Measure measure;
  private Alert alert;

  @Before
  public void setup() {
    metric = new Metric.Builder("test-metric", "name", Metric.ValueType.FLOAT).create();
    measure = new Measure();
    measure.setMetric(metric);
    alert = new Alert();
  }

  @Test
  public void testInputNumbers() {
    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    alert.setOperator(Alert.OPERATOR_SMALLER);
    alert.setMetric(metric);

    try {
      metric.setType(Metric.ValueType.FLOAT);
      alert.setValueError("20");
      AlertUtils.getLevel(alert, measure);
    } catch (NumberFormatException ex) {
      Assert.fail();
    }

    try {
      metric.setType(Metric.ValueType.INT);
      alert.setValueError("20.1");
      AlertUtils.getLevel(alert, measure);
    } catch (NumberFormatException ex) {
      Assert.fail();
    }

    try {
      metric.setType(Metric.ValueType.PERCENT);
      alert.setValueError("20.1");
      AlertUtils.getLevel(alert, measure);
    } catch (NumberFormatException ex) {
      Assert.fail();
    }
  }

  @Test
  public void testEquals() {

    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    alert.setOperator(Alert.OPERATOR_EQUALS);
    alert.setMetric(metric);

    alert.setValueError("10.2");
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));

    alert.setValueError("10.1");
    Assert.assertEquals(Metric.Level.OK, AlertUtils.getLevel(alert, measure));

    metric.setType(Metric.ValueType.STRING);
    measure.setData("TEST");
    measure.setValue(null);

    alert.setValueError("TEST");
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));

    alert.setValueError("TEST2");
    Assert.assertEquals(Metric.Level.OK, AlertUtils.getLevel(alert, measure));

  }

  @Test
  public void testNotEquals() {

    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    alert.setOperator(Alert.OPERATOR_NOT_EQUALS);
    alert.setMetric(metric);

    alert.setValueError("10.2");
    Assert.assertEquals(Metric.Level.OK, AlertUtils.getLevel(alert, measure));

    alert.setValueError("10.1");
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));

    metric.setType(Metric.ValueType.STRING);
    measure.setData("TEST");
    measure.setValue(null);

    alert.setValueError("TEST");
    Assert.assertEquals(Metric.Level.OK, AlertUtils.getLevel(alert, measure));

    alert.setValueError("TEST2");
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));

  }

  @Test
  public void testGreater() {
    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    alert.setOperator(Alert.OPERATOR_GREATER);
    alert.setMetric(metric);

    alert.setValueError("10.1");
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));

    alert.setValueError("10.3");
    Assert.assertEquals(Metric.Level.OK, AlertUtils.getLevel(alert, measure));
  }

  @Test
  public void testSmaller() {
    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    alert.setOperator(Alert.OPERATOR_SMALLER);
    alert.setMetric(metric);

    alert.setValueError("10.1");
    Assert.assertEquals(Metric.Level.OK, AlertUtils.getLevel(alert, measure));

    alert.setValueError("10.3");
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));
  }

  @Test
  public void testPercent() {
    metric.setType(Metric.ValueType.PERCENT);
    measure.setValue(10.2d);
    alert.setOperator(Alert.OPERATOR_EQUALS);
    alert.setMetric(metric);

    alert.setValueError("10.2");
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));
  }

  @Test
  public void testFloat() {
    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    alert.setOperator(Alert.OPERATOR_EQUALS);
    alert.setMetric(metric);

    alert.setValueError("10.2");
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));
  }

  @Test
  public void testInteger() {
    metric.setType(Metric.ValueType.INT);
    measure.setValue(10.2d);
    alert.setOperator(Alert.OPERATOR_EQUALS);
    alert.setMetric(metric);

    alert.setValueError("10");
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));

    alert.setValueError("10.2");
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));
  }

  @Test
  public void testLevel() {
    metric.setType(Metric.ValueType.LEVEL);
    measure.setData(Metric.Level.ERROR.toString());
    alert.setOperator(Alert.OPERATOR_EQUALS);
    alert.setMetric(metric);

    alert.setValueError(Metric.Level.ERROR.toString());
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));

    alert.setValueError(Metric.Level.OK.toString());
    Assert.assertEquals(Metric.Level.OK, AlertUtils.getLevel(alert, measure));

    alert.setOperator(Alert.OPERATOR_NOT_EQUALS);
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));
  }

  @Test
  public void testBooleans() {
    metric.setType(Metric.ValueType.BOOL);
    measure.setValue(0d);
    alert.setOperator(Alert.OPERATOR_EQUALS);
    alert.setMetric(metric);

    alert.setValueError("1");
    Assert.assertEquals(Metric.Level.OK, AlertUtils.getLevel(alert, measure));

    alert.setValueError("0");
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));

    alert.setOperator(Alert.OPERATOR_NOT_EQUALS);
    alert.setValueError("1");
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));

    alert.setValueError("0");
    Assert.assertEquals(Metric.Level.OK, AlertUtils.getLevel(alert, measure));
  }

  @Test
  public void test_work_duration() {
    metric.setType(Metric.ValueType.WORK_DUR);
    measure.setValue(60.0d);
    alert.setOperator(Alert.OPERATOR_EQUALS);
    alert.setMetric(metric);

    alert.setValueError("60");
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));
  }

  @Test
  public void testErrorAndWarningLevel() {
    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    alert.setOperator(Alert.OPERATOR_EQUALS);
    alert.setMetric(metric);

    alert.setValueError("10.2");
    Assert.assertEquals(Metric.Level.ERROR, AlertUtils.getLevel(alert, measure));

    alert.setValueError("10.1");
    Assert.assertEquals(Metric.Level.OK, AlertUtils.getLevel(alert, measure));

    alert.setValueError("10.3");
    alert.setValueWarning("10.2");
    Assert.assertEquals(Metric.Level.WARN, AlertUtils.getLevel(alert, measure));
  }
}
