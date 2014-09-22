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
package org.sonar.api.measures;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

public class MeasureUtilsTest {

  @Test
  public void getValue() {
    assertThat(MeasureUtils.getValue(null, 3.0), is(3.0));
    assertThat(MeasureUtils.getValue(new Measure(), 3.0), is(3.0));
    assertThat(MeasureUtils.getValue(new Measure(CoreMetrics.LINES, 2.0), 3.0), is(2.0));
    assertThat(MeasureUtils.getValue(new Measure(CoreMetrics.LINES, "data"), 3.0), is(3.0));
  }

  @Test
  public void sumNone() {
    assertThat(MeasureUtils.sum(true), is(0d));
    assertNull(MeasureUtils.sum(false));
  }

  @Test
  public void shouldNotFailIfDataMeasures() {
    assertThat(MeasureUtils.sum(true, new Measure(CoreMetrics.ALERT_STATUS, "foo"), new Measure(CoreMetrics.LINES, 50.0)), is(50d));
  }

  @Test
  public void sumNumericMeasures() {
    assertThat(MeasureUtils.sum(true, new Measure(CoreMetrics.LINES, 80.0), new Measure(CoreMetrics.LINES, 50.0)), is(130d));
    assertThat(MeasureUtils.sum(true, Arrays.asList(new Measure(CoreMetrics.LINES, 80.0), new Measure(CoreMetrics.LINES, 50.0))), is(130d));
  }

  @Test
  public void sumNullMeasures() {
    assertThat(MeasureUtils.sum(true), is(0.0));
    assertThat(MeasureUtils.sum(true, (Collection<Measure>) null), is(0.0));
    assertThat(MeasureUtils.sum(false), nullValue());
    assertThat(MeasureUtils.sum(true, new Measure(CoreMetrics.LINES, 80.0), null, null, new Measure(CoreMetrics.LINES, 50.0)), is(130d));
  }

  @Test
  public void hasValue() {
    assertThat(MeasureUtils.hasValue(null), is(false));
    assertThat(MeasureUtils.hasValue(new Measure(CoreMetrics.CLASSES, (Double) null)), is(false));
    assertThat(MeasureUtils.hasValue(new Measure(CoreMetrics.CLASSES, 3.2)), is(true));
  }

  @Test
  public void hasData() {
    assertThat(MeasureUtils.hasData(null), is(false));
    assertThat(MeasureUtils.hasData(new Measure(CoreMetrics.CLASSES, 3.5)), is(false));
    assertThat(MeasureUtils.hasData(new Measure(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, (String) null)), is(false));
    assertThat(MeasureUtils.hasData(new Measure(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, "")), is(false));
    assertThat(MeasureUtils.hasData(new Measure(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, "1=10;2=20")), is(true));
  }

  @Test
  public void shouldHaveValues() {
    assertThat(MeasureUtils.haveValues(), is(false));
    assertThat(MeasureUtils.haveValues(null, null), is(false));
    assertThat(MeasureUtils.haveValues(new Measure(CoreMetrics.CLASSES, (Double) null)), is(false));
    assertThat(MeasureUtils.haveValues(new Measure(CoreMetrics.CLASSES, 3.2)), is(true));
    assertThat(MeasureUtils.haveValues(new Measure(CoreMetrics.CLASSES, 3.2), new Measure(CoreMetrics.COMPLEXITY, "foo")), is(false));
    assertThat(MeasureUtils.haveValues(new Measure(CoreMetrics.CLASSES, 3.2), new Measure(CoreMetrics.COMPLEXITY, 2.5)), is(true));
  }

  @Test
  public void shouldGetVariation() {
    assertThat(MeasureUtils.getVariation(null, 2, 3.14), is(3.14));
    assertThat(MeasureUtils.getVariation(null, 2), nullValue());

    assertThat(MeasureUtils.getVariation(new Measure(), 2, 3.14), is(3.14));
    assertThat(MeasureUtils.getVariation(new Measure(), 2), nullValue());

    assertThat(MeasureUtils.getVariation(new Measure().setVariation2(1.618), 2, 3.14), is(1.618));
  }

  @Test
  public void shouldGetVariationAsLong() {
    assertThat(MeasureUtils.getVariationAsLong(null, 2, 3L), is(3L));
    assertThat(MeasureUtils.getVariationAsLong(null, 2), nullValue());

    assertThat(MeasureUtils.getVariationAsLong(new Measure(), 2, 3L), is(3L));
    assertThat(MeasureUtils.getVariationAsLong(new Measure(), 2), nullValue());

    assertThat(MeasureUtils.getVariationAsLong(new Measure().setVariation2(222.0), 2, 3L), is(222L));
  }

  @Test
  public void shouldSumOnVariation() throws Exception {
    Measure measure1 = new Measure(CoreMetrics.NEW_VIOLATIONS).setVariation1(1.0).setVariation2(1.0).setVariation3(3.0);
    Measure measure2 = new Measure(CoreMetrics.NEW_VIOLATIONS).setVariation1(1.0).setVariation2(2.0).setVariation3(3.0);
    List<Measure> children = Arrays.asList(measure1, measure2);

    assertThat(MeasureUtils.sumOnVariation(true, 1, children), is(2d));
    assertThat(MeasureUtils.sumOnVariation(true, 2, children), is(3d));
    assertThat(MeasureUtils.sumOnVariation(true, 3, children), is(6d));
  }

}
