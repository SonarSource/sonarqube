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

package org.sonar.core.measure.db;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class MeasureKeyTest {

  @Test
  public void create_key() throws Exception {
    MeasureKey key = MeasureKey.of("sample", "ncloc");
    assertThat(key.componentKey()).isEqualTo("sample");
    assertThat(key.metricKey()).isEqualTo("ncloc");
  }

  @Test
  public void component_key_must_not_be_null() throws Exception {
    try {
      MeasureKey.of(null, "ncloc");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Component key must be set");
    }
  }

  @Test
  public void metric_key_must_not_be_null() throws Exception {
    try {
      MeasureKey.of("sample", null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Metric key must be set");
    }
  }

  @Test
  public void test_equals_and_hashcode() throws Exception {
    MeasureKey key1 = MeasureKey.of("sample", "ncloc");
    MeasureKey key2 = MeasureKey.of("sample", "ncloc");
    MeasureKey key3 = MeasureKey.of("sample", "coverage");
    MeasureKey key4 = MeasureKey.of("sample2", "coverage");

    assertThat(key1).isEqualTo(key1);
    assertThat(key1).isEqualTo(key2);
    assertThat(key1).isNotEqualTo(key3);
    assertThat(key3).isNotEqualTo(key4);
    assertThat(key1).isNotEqualTo(null);
    assertThat(key1.hashCode()).isEqualTo(key1.hashCode());
    assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
  }
}
