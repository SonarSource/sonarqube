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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasureUtilsTest {

  @Test
  public void getValue() {
    assertThat(MeasureUtils.getValue(null, 3.0)).isEqualTo(3.0);
    assertThat(MeasureUtils.getValue(new Measure(), 3.0)).isEqualTo(3.0);
    assertThat(MeasureUtils.getValue(new Measure(CoreMetrics.LINES, 2.0), 3.0)).isEqualTo(2.0);
    assertThat(MeasureUtils.getValue(new Measure(CoreMetrics.LINES, "data"), 3.0)).isEqualTo(3.0);
  }

  @Test
  public void sumNone() {
    assertThat(MeasureUtils.sum(true)).isEqualTo(0d);
    assertThat(MeasureUtils.sum(false)).isNull();
  }

  @Test
  public void shouldNotFailIfDataMeasures() {
    assertThat(MeasureUtils.sum(true, new Measure(CoreMetrics.ALERT_STATUS, "foo"), new Measure(CoreMetrics.LINES, 50.0))).isEqualTo(50d);
  }

  @Test
  public void sumNumericMeasures() {
    assertThat(MeasureUtils.sum(true, new Measure(CoreMetrics.LINES, 80.0), new Measure(CoreMetrics.LINES, 50.0))).isEqualTo(130d);
    assertThat(MeasureUtils.sum(true, Arrays.asList(new Measure(CoreMetrics.LINES, 80.0), new Measure(CoreMetrics.LINES, 50.0)))).isEqualTo(130d);
  }

  @Test
  public void sumNullMeasures() {
    assertThat(MeasureUtils.sum(true)).isEqualTo(0.0);
    assertThat(MeasureUtils.sum(true, (Collection<Measure>) null)).isEqualTo(0.0);
    assertThat(MeasureUtils.sum(false)).isNull();
    assertThat(MeasureUtils.sum(true, new Measure(CoreMetrics.LINES, 80.0), null, null, new Measure(CoreMetrics.LINES, 50.0))).isEqualTo(130d);
  }

  @Test
  public void hasValue() {
    assertThat(MeasureUtils.hasValue(null)).isFalse();
    assertThat(MeasureUtils.hasValue(new Measure(CoreMetrics.CLASSES, (Double) null))).isFalse();
    assertThat(MeasureUtils.hasValue(new Measure(CoreMetrics.CLASSES, 3.2))).isTrue();
  }

  @Test
  public void hasData() {
    assertThat(MeasureUtils.hasData(null)).isFalse();
    assertThat(MeasureUtils.hasData(new Measure(CoreMetrics.CLASSES, 3.5))).isFalse();
    assertThat(MeasureUtils.hasData(new Measure(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, (String) null))).isFalse();
    assertThat(MeasureUtils.hasData(new Measure(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, ""))).isFalse();
    assertThat(MeasureUtils.hasData(new Measure(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, "1=10;2=20"))).isTrue();
  }

  @Test
  public void shouldHaveValues() {
    assertThat(MeasureUtils.haveValues()).isFalse();
    assertThat(MeasureUtils.haveValues(null, null)).isFalse();
    assertThat(MeasureUtils.haveValues(new Measure(CoreMetrics.CLASSES, (Double) null))).isFalse();
    assertThat(MeasureUtils.haveValues(new Measure(CoreMetrics.CLASSES, 3.2))).isTrue();
    assertThat(MeasureUtils.haveValues(new Measure(CoreMetrics.CLASSES, 3.2), new Measure(CoreMetrics.COMPLEXITY, "foo"))).isFalse();
    assertThat(MeasureUtils.haveValues(new Measure(CoreMetrics.CLASSES, 3.2), new Measure(CoreMetrics.COMPLEXITY, 2.5))).isTrue();
  }

  @Test
  public void shouldGetVariation() {
    assertThat(MeasureUtils.getVariation(null, 2, 3.14)).isEqualTo(3.14);
    assertThat(MeasureUtils.getVariation(null, 2)).isNull();

    assertThat(MeasureUtils.getVariation(new Measure(), 2, 3.14)).isEqualTo(3.14);
    assertThat(MeasureUtils.getVariation(new Measure(), 2)).isNull();

    assertThat(MeasureUtils.getVariation(new Measure().setVariation2(1.618), 2, 3.14)).isEqualTo(1.618);
  }

  @Test
  public void shouldGetVariationAsLong() {
    assertThat(MeasureUtils.getVariationAsLong(null, 2, 3L)).isEqualTo(3L);
    assertThat(MeasureUtils.getVariationAsLong(null, 2)).isNull();

    assertThat(MeasureUtils.getVariationAsLong(new Measure(), 2, 3L)).isEqualTo(3L);
    assertThat(MeasureUtils.getVariationAsLong(new Measure(), 2)).isNull();

    assertThat(MeasureUtils.getVariationAsLong(new Measure().setVariation2(222.0), 2, 3L)).isEqualTo(222L);
  }

  @Test
  public void shouldSumOnVariation() {
    Measure measure1 = new Measure(CoreMetrics.NEW_VIOLATIONS).setVariation1(1.0).setVariation2(1.0).setVariation3(3.0);
    Measure measure2 = new Measure(CoreMetrics.NEW_VIOLATIONS).setVariation1(1.0).setVariation2(2.0).setVariation3(3.0);
    List<Measure> children = Arrays.asList(measure1, measure2);

    assertThat(MeasureUtils.sumOnVariation(true, 1, children)).isEqualTo(2d);
    assertThat(MeasureUtils.sumOnVariation(true, 2, children)).isEqualTo(3d);
    assertThat(MeasureUtils.sumOnVariation(true, 3, children)).isEqualTo(6d);
  }

}
