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

package org.sonar.server.computation.formula;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RangeDistributionBuilderTest {

  @Test
  public void build_integer_distribution() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();
    String data = builder
      .add("0=0;2=2;4=1")
      .add("0=1;2=2;4=2")
      .build().get();

    assertThat(data).isEqualTo("0=1;2=4;4=3");
  }

  @Test
  public void build_double_distribution() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();
    String data = builder
      .add("0.5=0;1.9=2;4.5=1")
      .add("0.5=1;1.9=3;4.5=1")
      .build().get();

    assertThat(data).isEqualTo("0.5=1;1.9=5;4.5=2");
  }

  @Test
  public void add_distribution_measure_with_identical_limits() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();
    String data = builder
      .add("0=1;2=0")
      .add("0=3;2=5")
      .build().get();

    assertThat(data).isEqualTo("0=4;2=5");
  }

  @Test
  public void add_distribution_measure_with_different_int_limits() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();

    assertThat(builder
      .add("0=1")
      .add("0=3;2=5")
      .build().isPresent()).isFalse();
  }

  @Test
  public void add_distribution_measure_with_different_double_limits() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();

    assertThat(builder
      .add("0.0=3;3.0=5")
      .add("0.0=3;3.0=5;6.0=9")
      .build().isPresent()).isFalse();
  }

  @Test
  public void init_limits_at_the_first_add() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();
    String data = builder
      .add("0.5=3;3.5=5;6.5=9")
      .add("0.5=0;3.5=2;6.5=1")
      .build().get();

    assertThat(data).isEqualTo("0.5=3;3.5=7;6.5=10");
  }

  @Test
  public void keep_int_ranges_when_merging_distributions() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();
    String data = builder
      .add("0=3;3=5;6=9")
      .add("0=0;3=2;6=1")
      .build().get();

    assertThat(data).isEqualTo("0=3;3=7;6=10");
  }

  @Test
  public void return_empty_string_when_empty_data() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();

    assertThat(builder.isEmpty()).isTrue();
    assertThat(builder.build().get()).isEmpty();
  }

  @Test
  public void aggregate_empty_distribution() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();
    String distribution = builder.build().get();
    assertThat(distribution).isEmpty();
  }

}
