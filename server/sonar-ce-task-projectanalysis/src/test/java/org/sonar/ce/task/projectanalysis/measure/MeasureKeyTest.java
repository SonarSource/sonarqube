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
package org.sonar.ce.task.projectanalysis.measure;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasureKeyTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void fail_with_NPE_when_metric_key_is_null() {
    thrown.expect(NullPointerException.class);

    new MeasureKey(null);
  }

  @Test
  public void test_equals_and_hashcode() {
    MeasureKey measureKey = new MeasureKey("metricKey");
    MeasureKey measureKey2 = new MeasureKey("metricKey");
    MeasureKey anotherMeasureKey = new MeasureKey("anotherMetricKey");

    assertThat(measureKey).isEqualTo(measureKey);
    assertThat(measureKey).isEqualTo(measureKey2);
    assertThat(measureKey).isNotEqualTo(null);
    assertThat(measureKey).isNotEqualTo(anotherMeasureKey);


    assertThat(measureKey.hashCode()).isEqualTo(measureKey.hashCode());
    assertThat(measureKey.hashCode()).isEqualTo(measureKey2.hashCode());
    assertThat(measureKey.hashCode()).isNotEqualTo(anotherMeasureKey.hashCode());
  }

  @Test
  public void to_string() {
    assertThat(new MeasureKey("metricKey").toString()).isEqualTo(
      "MeasureKey{metricKey='metricKey'}");
    assertThat(new MeasureKey("metricKey").toString()).isEqualTo("MeasureKey{metricKey='metricKey'}");
  }
}
