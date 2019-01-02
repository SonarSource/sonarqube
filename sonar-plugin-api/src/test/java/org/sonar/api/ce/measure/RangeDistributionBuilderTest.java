/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.ce.measure;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RangeDistributionBuilderTest {

  @Test
  public void work_on_an_limits_array_copy() {
    Integer[] limits = new Integer[] {4, 2, 0};
    RangeDistributionBuilder builder = new RangeDistributionBuilder(limits);
    builder.add(3.2).add(2.0).add(6.2).build();

    assertThat(limits[0]).isEqualTo(4);
    assertThat(limits[1]).isEqualTo(2);
    assertThat(limits[2]).isEqualTo(0);
  }

  @Test
  public void build_integer_distribution() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder(new Integer[] {0, 2, 4});
    String data = builder
      .add(3.2)
      .add(2.0)
      .add(6.2)
      .build();

    assertThat(data).isEqualTo("0=0;2=2;4=1");
  }

  @Test
  public void build_double_distribution() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder(new Double[] {0.0, 2.0, 4.0});
    String data = builder
      .add(3.2)
      .add(2.0)
      .add(6.2)
      .build();

    assertThat(data).isEqualTo("0=0;2=2;4=1");
  }

  @Test
  public void value_lesser_than_minimum_is_ignored() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder(new Integer[] {0, 2, 4});
    String data = builder
      .add(3.2)
      .add(2.0)
      .add(-3.0)
      .build();

    assertThat(data).isEqualTo("0=0;2=2;4=0");
  }

  @Test
  public void add_existing_integer_distribution() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();
    String data = builder
      .add("0=0;2=2;4=1")
      .add("0=1;2=2;4=2")
      .build();

    assertThat(data).isEqualTo("0=1;2=4;4=3");
  }

  @Test
  public void add_existing_double_distribution() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();
    String data = builder
      .add("0.5=0;1.9=2;4.5=1")
      .add("0.5=1;1.9=3;4.5=1")
      .build();

    assertThat(data).isEqualTo("0.5=1;1.9=5;4.5=2");
  }

  @Test
  public void add_distribution_with_identical_limits() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();
    String data = builder
      .add("0=1;2=0")
      .add("0=3;2=5")
      .build();

    assertThat(data).isEqualTo("0=4;2=5");
  }

  @Test
  public void add_distribution_with_different_int_limits() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();

    assertThat(builder
      .add("0=1")
      .add("0=3;2=5")
      .build()).isNull();
  }

  @Test
  public void add_distribution_with_different_double_limits() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();

    assertThat(builder
      .add("0.0=3;3.0=5")
      .add("0.0=3;3.0=5;6.0=9")
      .build()).isNull();
  }

  @Test
  public void init_limits_at_the_first_add() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();
    String data = builder
      .add("0.5=3;3.5=5;6.5=9")
      .add("0.5=0;3.5=2;6.5=1")
      .build();

    assertThat(data).isEqualTo("0.5=3;3.5=7;6.5=10");
  }

  @Test
  public void keep_int_ranges_when_merging_distributions() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();
    String data = builder
      .add("0=3;3=5;6=9")
      .add("0=0;3=2;6=1")
      .build();

    assertThat(data).isEqualTo("0=3;3=7;6=10");
  }


  @Test
  public void is_empty_is_true_when_no_data() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();

    assertThat(builder.isEmpty()).isTrue();
  }

  @Test
  public void is_empty_is_true_when_no_data_on_distribution_with_limits() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder(new Integer[] {4, 2, 0});

    assertThat(builder.isEmpty()).isTrue();
  }

  @Test
  public void aggregate_empty_distribution() {
    RangeDistributionBuilder builder = new RangeDistributionBuilder();
    String distribution = builder.build();
    assertThat(distribution).isEmpty();
  }

}
