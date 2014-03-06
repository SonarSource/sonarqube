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
import org.sonar.core.qualitygate.db.QualityGateConditionDto;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConditionUtilsTest {

  private Metric metric;
  private Measure measure;
  private ResolvedCondition condition;

  @Before
  public void setup() {
    metric = new Metric.Builder("test-metric", "name", Metric.ValueType.FLOAT).create();
    measure = new Measure();
    measure.setMetric(metric);
    condition = mock(ResolvedCondition.class);
    when(condition.period()).thenReturn(null);
  }

  @Test
  public void testInputNumbers() {
    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_LESS_THAN);
    when(condition.metric()).thenReturn(metric);

    try {
      metric.setType(Metric.ValueType.FLOAT);
      when(condition.errorThreshold()).thenReturn("20");
      ConditionUtils.getLevel(condition, measure);
    } catch (NumberFormatException ex) {
      Assert.fail();
    }

    try {
      metric.setType(Metric.ValueType.INT);
      when(condition.errorThreshold()).thenReturn("20.1");
      ConditionUtils.getLevel(condition, measure);
    } catch (NumberFormatException ex) {
      Assert.fail();
    }

    try {
      metric.setType(Metric.ValueType.PERCENT);
      when(condition.errorThreshold()).thenReturn("20.1");
      ConditionUtils.getLevel(condition, measure);
    } catch (NumberFormatException ex) {
      Assert.fail();
    }
  }

  @Test
  public void testEquals() {

    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10.2");
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));

    when(condition.errorThreshold()).thenReturn("10.1");
    Assert.assertEquals(Metric.Level.OK, ConditionUtils.getLevel(condition, measure));

    metric.setType(Metric.ValueType.STRING);
    measure.setData("TEST");
    measure.setValue(null);

    when(condition.errorThreshold()).thenReturn("TEST");
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));

    when(condition.errorThreshold()).thenReturn("TEST2");
    Assert.assertEquals(Metric.Level.OK, ConditionUtils.getLevel(condition, measure));

  }

  @Test
  public void testNotEquals() {

    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_NOT_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10.2");
    Assert.assertEquals(Metric.Level.OK, ConditionUtils.getLevel(condition, measure));

    when(condition.errorThreshold()).thenReturn("10.1");
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));

    metric.setType(Metric.ValueType.STRING);
    measure.setData("TEST");
    measure.setValue(null);

    when(condition.errorThreshold()).thenReturn("TEST");
    Assert.assertEquals(Metric.Level.OK, ConditionUtils.getLevel(condition, measure));

    when(condition.errorThreshold()).thenReturn("TEST2");
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));

  }

  @Test
  public void testGreater() {
    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_GREATER_THAN);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10.1");
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));

    when(condition.errorThreshold()).thenReturn("10.3");
    Assert.assertEquals(Metric.Level.OK, ConditionUtils.getLevel(condition, measure));
  }

  @Test
  public void testSmaller() {
    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_LESS_THAN);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10.1");
    Assert.assertEquals(Metric.Level.OK, ConditionUtils.getLevel(condition, measure));

    when(condition.errorThreshold()).thenReturn("10.3");
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));
  }

  @Test
  public void testPercent() {
    metric.setType(Metric.ValueType.PERCENT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10.2");
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));
  }

  @Test
  public void testFloat() {
    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10.2");
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));
  }

  @Test
  public void testInteger() {
    metric.setType(Metric.ValueType.INT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10");
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));

    when(condition.errorThreshold()).thenReturn("10.2");
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));
  }

  @Test
  public void testLevel() {
    metric.setType(Metric.ValueType.LEVEL);
    measure.setData(Metric.Level.ERROR.toString());
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn(Metric.Level.ERROR.toString());
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));

    when(condition.errorThreshold()).thenReturn(Metric.Level.OK.toString());
    Assert.assertEquals(Metric.Level.OK, ConditionUtils.getLevel(condition, measure));

    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_NOT_EQUALS);
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));
  }

  @Test
  public void testBooleans() {
    metric.setType(Metric.ValueType.BOOL);
    measure.setValue(0d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("1");
    Assert.assertEquals(Metric.Level.OK, ConditionUtils.getLevel(condition, measure));

    when(condition.errorThreshold()).thenReturn("0");
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));

    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_NOT_EQUALS);
    when(condition.errorThreshold()).thenReturn("1");
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));

    when(condition.errorThreshold()).thenReturn("0");
    Assert.assertEquals(Metric.Level.OK, ConditionUtils.getLevel(condition, measure));
  }

  @Test
  public void test_work_duration() {
    metric.setType(Metric.ValueType.WORK_DUR);
    measure.setValue(60.0d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("60");
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));
  }

  @Test
  public void testErrorAndWarningLevel() {
    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10.2");
    Assert.assertEquals(Metric.Level.ERROR, ConditionUtils.getLevel(condition, measure));

    when(condition.errorThreshold()).thenReturn("10.1");
    Assert.assertEquals(Metric.Level.OK, ConditionUtils.getLevel(condition, measure));

    when(condition.errorThreshold()).thenReturn("10.3");
    when(condition.warningThreshold()).thenReturn("10.2");
    Assert.assertEquals(Metric.Level.WARN, ConditionUtils.getLevel(condition, measure));
  }
}
