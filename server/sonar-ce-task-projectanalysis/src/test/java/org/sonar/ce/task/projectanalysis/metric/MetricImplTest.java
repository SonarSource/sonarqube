/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.metric;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MetricImplTest {

  private static final String SOME_UUID = "uuid";
  private static final String SOME_KEY = "key";
  private static final String SOME_NAME = "name";

  @Test
  public void constructor_throws_NPE_if_key_arg_is_null() {
    assertThatThrownBy(() -> new MetricImpl(SOME_UUID, null, SOME_NAME, Metric.MetricType.BOOL))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void constructor_throws_NPE_if_name_arg_is_null() {
    assertThatThrownBy(() -> new MetricImpl(SOME_UUID, SOME_KEY, null, Metric.MetricType.BOOL))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void constructor_throws_NPE_if_valueType_arg_is_null() {
    assertThatThrownBy(() -> new MetricImpl(SOME_UUID, SOME_KEY, SOME_NAME, null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void constructor_throws_IAE_if_bestValueOptimized_is_true_but_bestValue_is_null() {
    assertThatThrownBy(() -> new MetricImpl(SOME_UUID, SOME_KEY, SOME_NAME, Metric.MetricType.INT, 1, null, true, false))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("A BestValue must be specified if Metric is bestValueOptimized");
  }

  @Test
  public void verify_getters() {
    MetricImpl metric = new MetricImpl(SOME_UUID, SOME_KEY, SOME_NAME, Metric.MetricType.FLOAT);

    assertThat(metric.getUuid()).isEqualTo(SOME_UUID);
    assertThat(metric.getKey()).isEqualTo(SOME_KEY);
    assertThat(metric.getName()).isEqualTo(SOME_NAME);
    assertThat(metric.getType()).isEqualTo(Metric.MetricType.FLOAT);
  }

  @Test
  public void equals_uses_only_key() {
    MetricImpl expected = new MetricImpl(SOME_UUID, SOME_KEY, SOME_NAME, Metric.MetricType.FLOAT);

    assertThat(new MetricImpl(SOME_UUID, SOME_KEY, SOME_NAME, Metric.MetricType.FLOAT)).isEqualTo(expected);
    assertThat(new MetricImpl(SOME_UUID, SOME_KEY, SOME_NAME, Metric.MetricType.STRING)).isEqualTo(expected);
    assertThat(new MetricImpl(SOME_UUID, SOME_KEY, SOME_NAME, Metric.MetricType.STRING, null, 0d, true, true)).isEqualTo(expected);
    assertThat(new MetricImpl(SOME_UUID, SOME_KEY, SOME_NAME, Metric.MetricType.STRING, null, null, false, false)).isEqualTo(expected);
    assertThat(new MetricImpl(SOME_UUID, "some other key", SOME_NAME, Metric.MetricType.FLOAT)).isNotEqualTo(expected);
  }

  @Test
  public void hashcode_uses_only_key() {
    int expected = new MetricImpl(SOME_UUID, SOME_KEY, SOME_NAME, Metric.MetricType.FLOAT).hashCode();

    assertThat(new MetricImpl(SOME_UUID, SOME_KEY, "some other name", Metric.MetricType.FLOAT).hashCode()).isEqualTo(expected);
    assertThat(new MetricImpl(SOME_UUID, SOME_KEY, "some other name", Metric.MetricType.BOOL).hashCode()).isEqualTo(expected);
  }

  @Test
  public void all_fields_are_displayed_in_toString() {
    assertThat(new MetricImpl(SOME_UUID, SOME_KEY, SOME_NAME, Metric.MetricType.FLOAT, 1, 951d, true, false))
      .hasToString("MetricImpl{uuid=uuid, key=key, name=name, type=FLOAT, bestValue=951.0, bestValueOptimized=true, deleteHistoricalData=false}");

  }
}
