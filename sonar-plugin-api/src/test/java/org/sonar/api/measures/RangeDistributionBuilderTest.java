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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RangeDistributionBuilderTest {

  @Test
  public void workOnAnLimitsArrayCopy() {
    Integer[] limits = new Integer[] {4, 2, 0};
    RangeDistributionBuilder builder = new RangeDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION, limits);
    builder.add(3.2).add(2.0).add(6.2).build();

    assertThat(builder.getBottomLimits()).isNotSameAs(limits);
    assertThat(limits[0]).isEqualTo(4);
    assertThat(limits[1]).isEqualTo(2);
    assertThat(limits[2]).isEqualTo(0);
  }

  @Test
  public void buildIntegerDistribution() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION, new Integer[] {0, 2, 4});
    Measure measure = builder
      .add(3.2)
      .add(2.0)
      .add(6.2)
      .build();

    assertThat(measure.getData()).isEqualTo("0=0;2=2;4=1");
  }

  @Test
  public void buildDoubleDistribution() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION, new Double[] {0.0, 2.0, 4.0});
    Measure measure = builder
      .add(3.2)
      .add(2.0)
      .add(6.2)
      .build();

    assertThat(measure.getData()).isEqualTo("0=0;2=2;4=1");
  }

  @Test
  public void valueLesserThanMinimumIsIgnored() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION, new Integer[] {0, 2, 4});
    Measure measure = builder
      .add(3.2)
      .add(2.0)
      .add(-3.0)
      .build();

    assertThat(measure.getData()).isEqualTo("0=0;2=2;4=0");
  }

  @Test
  public void addDistributionMeasureWithIdenticalLimits() {
    Measure measureToAdd = mock(Measure.class);
    when(measureToAdd.getData()).thenReturn("0=3;2=5");

    RangeDistributionBuilder builder = new RangeDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION, new Integer[] {0, 2});
    builder.clear();
    Measure measure = builder
      .add(1)
      .add(measureToAdd)
      .build();

    assertThat(measure.getData()).isEqualTo("0=4;2=5");
  }

  @Test
  public void addDistributionMeasureWithDifferentIntLimits() {
    Measure measureToAdd = mock(Measure.class);
    when(measureToAdd.getData()).thenReturn("0=3;2=5");

    RangeDistributionBuilder builder = new RangeDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION, new Integer[] {0, 2, 4});
    builder.clear();
    Measure measure = builder
      .add(1)
      .add(measureToAdd)
      .build();

    assertThat(measure).isNull();
  }

  @Test
  public void addDistributionMeasureWithDifferentDoubleLimits() {
    Measure measureToAdd = mock(Measure.class);
    when(measureToAdd.getData()).thenReturn("0.0=3;3.0=5;6.0=9");

    RangeDistributionBuilder builder = new RangeDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION, new Double[] {0.0, 2.0, 4.0});
    builder.clear();
    Measure measure = builder
      .add(measureToAdd)
      .build();

    assertThat(measure).isNull();
  }

  @Test
  public void initLimitsAtTheFirstAdd() {
    Measure m1 = mock(Measure.class);
    when(m1.getData()).thenReturn("0.5=3;3.5=5;6.5=9");

    Measure m2 = mock(Measure.class);
    when(m2.getData()).thenReturn("0.5=0;3.5=2;6.5=1");

    RangeDistributionBuilder builder = new RangeDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION);
    builder.clear();
    Measure measure = builder
      .add(m1)
      .add(m2)
      .build();

    assertThat(measure.getData()).isEqualTo("0.5=3;3.5=7;6.5=10");
  }

  @Test
  public void keepIntRangesWhenMergingDistributions() {
    Measure m1 = mock(Measure.class);
    when(m1.getData()).thenReturn("0=3;3=5;6=9");

    Measure m2 = mock(Measure.class);
    when(m2.getData()).thenReturn("0=0;3=2;6=1");

    RangeDistributionBuilder builder = new RangeDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION);
    builder.clear();
    Measure measure = builder
      .add(m1)
      .add(m2)
      .build();

    assertThat(measure.getData()).isEqualTo("0=3;3=7;6=10");
  }

  @Test
  public void nullIfEmptyData() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION, new Integer[] {0, 2, 4});

    assertThat(builder.isEmpty()).isTrue();
    Measure measure = builder.build(false);
    assertThat(measure).isNull();

    measure = builder.build(true);
    assertThat(measure.getData()).isEqualTo("0=0;2=0;4=0");
  }

  @Test
  public void aggregateEmptyDistribution() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION);
    builder.add(new Measure(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION, (String) null));
    Measure distribution = builder.build();
    assertThat(distribution.getData()).isEmpty();
  }
}
