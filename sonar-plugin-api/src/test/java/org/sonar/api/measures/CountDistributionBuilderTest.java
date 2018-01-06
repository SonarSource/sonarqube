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

public class CountDistributionBuilderTest {
  @Test
  public void buildDistribution() {
    CountDistributionBuilder builder = new CountDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION);
    Measure measure = builder
      .add("foo")
      .add("bar")
      .add("foo")
      .add("hello")
      .build();

    assertThat(measure.getData()).isEqualTo("bar=1;foo=2;hello=1");
  }

  @Test
  public void addZeroValues() {
    CountDistributionBuilder builder = new CountDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION);
    Measure measure = builder
      .addZero("foo")
      .add("bar")
      .add("foo")
      .addZero("hello")
      .build();

    assertThat(measure.getData()).isEqualTo("bar=1;foo=1;hello=0");
  }

  @Test
  public void addDistributionMeasureAsStrings() {
    Measure measureToAdd = mock(Measure.class);
    when(measureToAdd.getData()).thenReturn("foo=3;hello=5;none=0");

    CountDistributionBuilder builder = new CountDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION);
    Measure measure = builder
      .add("bar")
      .add("foo")
      .add(measureToAdd)
      .build();

    assertThat(measure.getData()).isEqualTo("bar=1;foo=4;hello=5;none=0");
  }

  @Test
  public void intervalsAreSorted() {
    Measure measureToAdd = mock(Measure.class);
    when(measureToAdd.getData()).thenReturn("10=5;3=2;1=3");

    CountDistributionBuilder builder = new CountDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION);
    Measure measure = builder
      .add(10)
      .add(measureToAdd)
      .add(1)
      .build();

    assertThat(measure.getData()).isEqualTo("1=4;3=2;10=6");
  }

}
