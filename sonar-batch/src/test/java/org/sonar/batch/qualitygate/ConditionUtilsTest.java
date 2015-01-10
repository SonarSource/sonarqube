/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.core.qualitygate.db.QualityGateConditionDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
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
      fail();
    }

    try {
      metric.setType(Metric.ValueType.INT);
      when(condition.errorThreshold()).thenReturn("20.1");
      ConditionUtils.getLevel(condition, measure);
    } catch (NumberFormatException ex) {
      fail();
    }

    try {
      metric.setType(Metric.ValueType.PERCENT);
      when(condition.errorThreshold()).thenReturn("20.1");
      ConditionUtils.getLevel(condition, measure);
    } catch (NumberFormatException ex) {
      fail();
    }
  }

  @Test
  public void testEquals() {

    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10.2");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);

    when(condition.errorThreshold()).thenReturn("10.1");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.OK);

    metric.setType(Metric.ValueType.STRING);
    measure.setData("TEST");
    measure.setValue(null);

    when(condition.errorThreshold()).thenReturn("TEST");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);

    when(condition.errorThreshold()).thenReturn("TEST2");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.OK);

  }

  @Test
  public void testNotEquals() {

    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_NOT_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10.2");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.OK);

    when(condition.errorThreshold()).thenReturn("10.1");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);

    metric.setType(Metric.ValueType.STRING);
    measure.setData("TEST");
    measure.setValue(null);

    when(condition.errorThreshold()).thenReturn("TEST");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.OK);

    when(condition.errorThreshold()).thenReturn("TEST2");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);

  }

  @Test
  public void testGreater() {
    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_GREATER_THAN);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10.1");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);

    when(condition.errorThreshold()).thenReturn("10.3");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.OK);
  }

  @Test
  public void testSmaller() {
    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_LESS_THAN);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10.1");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.OK);

    when(condition.errorThreshold()).thenReturn("10.3");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);
  }

  @Test
  public void testPercent() {
    metric.setType(Metric.ValueType.PERCENT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10.2");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);
  }

  @Test
  public void testFloat() {
    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10.2");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);
  }

  @Test
  public void testInteger() {
    metric.setType(Metric.ValueType.INT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);

    when(condition.errorThreshold()).thenReturn("10.2");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);
  }

  @Test
  public void testLevel() {
    metric.setType(Metric.ValueType.LEVEL);
    measure.setData(Metric.Level.ERROR.toString());
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn(Metric.Level.ERROR.toString());
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);

    when(condition.errorThreshold()).thenReturn(Metric.Level.OK.toString());
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.OK);

    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_NOT_EQUALS);
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);
  }

  @Test
  public void testBooleans() {
    metric.setType(Metric.ValueType.BOOL);
    measure.setValue(0d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("1");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.OK);

    when(condition.errorThreshold()).thenReturn("0");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);

    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_NOT_EQUALS);
    when(condition.errorThreshold()).thenReturn("1");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);

    when(condition.errorThreshold()).thenReturn("0");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.OK);

    when(condition.errorThreshold()).thenReturn("polop");
    try {
      ConditionUtils.getLevel(condition, measure);
      fail();
    } catch(Exception expected) {
      assertThat(expected).isInstanceOf(IllegalArgumentException.class).hasMessage("Quality Gate: Unable to parse value 'polop' to compare against name");
    }
  }

  @Test
  public void test_work_duration() {
    metric.setType(Metric.ValueType.WORK_DUR);
    measure.setValue(60.0d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("60");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);

    when(condition.errorThreshold()).thenReturn("polop");
    try {
      ConditionUtils.getLevel(condition, measure);
      fail();
    } catch(Exception expected) {
      assertThat(expected).isInstanceOf(IllegalArgumentException.class).hasMessage("Quality Gate: Unable to parse value 'polop' to compare against name");
    }
  }

  @Test
  public void testErrorAndWarningLevel() {
    metric.setType(Metric.ValueType.FLOAT);
    measure.setValue(10.2d);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("10.2");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.ERROR);

    when(condition.errorThreshold()).thenReturn("10.1");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.OK);

    when(condition.errorThreshold()).thenReturn("10.3");
    when(condition.warningThreshold()).thenReturn("10.2");
    assertThat(ConditionUtils.getLevel(condition, measure)).isEqualTo(Metric.Level.WARN);
  }

  @Test
  public void testUnsupportedType() {
    metric.setType(Metric.ValueType.DATA);
    measure.setValue(3.14159265358);
    when(condition.operator()).thenReturn(QualityGateConditionDto.OPERATOR_EQUALS);
    when(condition.metric()).thenReturn(metric);

    when(condition.errorThreshold()).thenReturn("1.60217657");
    try {
      ConditionUtils.getLevel(condition, measure);
      fail();
    } catch (Exception expected) {
      assertThat(expected).isInstanceOf(NotImplementedException.class);
    }
  }

}
