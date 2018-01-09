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
package org.sonar.api.measures;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasureTest {

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void valueCanBeNull() {
    Measure measure = new Measure("metric_key").setValue(null);
    assertThat(measure.getValue()).isNull();
  }

  @Test
  public void valueShouldNotBeNaN() {
    thrown.expect(IllegalArgumentException.class);
    new Measure("metric_key").setValue(Double.NaN);
  }

  @Test
  public void measureWithLevelValue() {
    assertThat(new Measure(CoreMetrics.ALERT_STATUS, Metric.Level.ERROR).getData()).isEqualTo("ERROR");
    assertThat(new Measure(CoreMetrics.ALERT_STATUS, Metric.Level.ERROR).getDataAsLevel()).isEqualTo(Metric.Level.ERROR);
    assertThat(new Measure(CoreMetrics.ALERT_STATUS).setData(Metric.Level.ERROR).getDataAsLevel()).isEqualTo(Metric.Level.ERROR);
  }

  @Test
  public void measureWithIntegerValue() {
    assertThat(new Measure(CoreMetrics.LINES).setIntValue(3).getValue()).isEqualTo(3.0);
    assertThat(new Measure(CoreMetrics.LINES).setIntValue(null).getValue()).isNull();

    assertThat(new Measure(CoreMetrics.LINES).setIntValue(3).getIntValue()).isEqualTo(3);
    assertThat(new Measure(CoreMetrics.LINES).setIntValue(null).getIntValue()).isNull();

    assertThat(new Measure(CoreMetrics.LINES).setValue(3.6).getIntValue()).isEqualTo(3);
  }

  /**
   * Proper definition of equality for measures is important, because used to store them.
   */
  @Test
  public void equalsAndHashCode() {
    Measure measure1 = new Measure();
    Measure measure2 = new Measure();

    assertThat(measure1.equals(null)).isFalse();

    // another class
    assertThat(measure1.equals("")).isFalse();

    // same instance
    assertThat(measure1.equals(measure1)).isTrue();
    assertThat(measure1.hashCode()).isEqualTo(measure2.hashCode());

    // same key - null
    assertThat(measure1.equals(measure2)).isTrue();
    assertThat(measure2.equals(measure1)).isTrue();
    assertThat(measure1.hashCode()).isEqualTo(measure2.hashCode());

    // different keys
    measure1.setMetric(CoreMetrics.COVERAGE);
    assertThat(measure1.equals(measure2)).isFalse();
    assertThat(measure2.equals(measure1)).isFalse();
    assertThat(measure1.hashCode()).isNotEqualTo(measure2.hashCode());

    measure2.setMetric(CoreMetrics.LINES);
    assertThat(measure1.equals(measure2)).isFalse();
    assertThat(measure2.equals(measure1)).isFalse();
    assertThat(measure1.hashCode()).isNotEqualTo(measure2.hashCode());

    // same key
    measure2.setMetric(CoreMetrics.COVERAGE);
    assertThat(measure1.equals(measure2)).isTrue();
    assertThat(measure2.equals(measure1)).isTrue();
    assertThat(measure1.hashCode()).isEqualTo(measure2.hashCode());

    // value doesn't matter
    measure1.setValue(1.0);
    measure2.setValue(2.0);
    assertThat(measure1.equals(measure2)).isTrue();
    assertThat(measure2.equals(measure1)).isTrue();
    assertThat(measure1.hashCode()).isEqualTo(measure2.hashCode());
  }

  @Test
  public void longDataForDataMetric() {
    new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA, StringUtils.repeat("x", Measure.MAX_TEXT_SIZE + 1));
  }

  @Test
  public void shouldGetAndSetVariations() {
    Measure measure = new Measure(CoreMetrics.LINES).setVariation1(1d).setVariation2(2d).setVariation3(3d);
    assertThat(measure.getVariation1()).isEqualTo(1d);
    assertThat(measure.getVariation2()).isEqualTo(2d);
    assertThat(measure.getVariation3()).isEqualTo(3d);
  }

  @Test
  public void shouldSetVariationsWithIndex() {
    Measure measure = new Measure(CoreMetrics.LINES).setVariation(2, 3.3);
    assertThat(measure.getVariation1()).isNull();
    assertThat(measure.getVariation2()).isEqualTo(3.3);
    assertThat(measure.getVariation3()).isNull();
  }

  @Test
  public void notEqualRuleMeasures() {
    Measure measure = new Measure(CoreMetrics.VIOLATIONS, 30.0);
    RuleMeasure ruleMeasure = new RuleMeasure(CoreMetrics.VIOLATIONS, new Rule("foo", "bar"), RulePriority.CRITICAL, 3);
    assertThat(measure.equals(ruleMeasure)).isFalse();
    assertThat(ruleMeasure.equals(measure)).isFalse();
  }

  @Test
  public void shouldUnsetData() {
    String data = "1=10;21=456";
    Measure measure = new Measure(CoreMetrics.CONDITIONS_BY_LINE).setData(data);
    assertThat(measure.hasData()).isTrue();
    assertThat(measure.getData()).isEqualTo(data);

    measure.unsetData();

    assertThat(measure.hasData()).isFalse();
    assertThat(measure.getData()).isNull();
  }

  @Test
  public void null_value_and_null_variations_should_be_considered_as_best_value() {
    assertThat(new Measure(CoreMetrics.VIOLATIONS).setVariation1(0.0).isBestValue()).isTrue();
    assertThat(new Measure(CoreMetrics.VIOLATIONS).setVariation1(1.0).isBestValue()).isFalse();
    assertThat(new Measure(CoreMetrics.VIOLATIONS).setVariation2(1.0).isBestValue()).isFalse();
    assertThat(new Measure(CoreMetrics.VIOLATIONS).setVariation3(1.0).isBestValue()).isFalse();
    assertThat(new Measure(CoreMetrics.VIOLATIONS).setVariation4(1.0).isBestValue()).isFalse();
    assertThat(new Measure(CoreMetrics.VIOLATIONS).setVariation5(1.0).isBestValue()).isFalse();
  }

  @Test
  public void testBooleanValue() {
    assertThat(new Measure(new Metric.Builder("foo", "Sample boolean", ValueType.BOOL).create()).setValue(1.0).value()).isEqualTo(Boolean.TRUE);
    assertThat(new Measure(new Metric.Builder("foo", "Sample boolean", ValueType.BOOL).create()).setValue(0.0).value()).isEqualTo(Boolean.FALSE);
  }

}
