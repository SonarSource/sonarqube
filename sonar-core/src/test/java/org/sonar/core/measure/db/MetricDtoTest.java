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

public class MetricDtoTest {

  @Test
  public void getters_and_setters() throws Exception {
    MetricDto metricDto = MetricDto.createFor("coverage")
      .setId(1)
      .setDescription("Coverage by unit tests")
      .setValueType("PERCENT")
      .setQualitative(true)
      .setUserManaged(false)
      .setWorstValue(0d)
      .setBestValue(100d)
      .setOptimizedBestValue(true)
      .setDirection(1)
      .setEnabled(true);

    assertThat(metricDto.getKey()).isEqualTo("coverage");
    assertThat(metricDto.getName()).isEqualTo("coverage");
    assertThat(metricDto.getId()).isEqualTo(1);
    assertThat(metricDto.getDescription()).isEqualTo("Coverage by unit tests");
    assertThat(metricDto.getValueType()).isEqualTo("PERCENT");
    assertThat(metricDto.isQualitative()).isTrue();
    assertThat(metricDto.isUserManaged()).isFalse();
    assertThat(metricDto.getWorstValue()).isEqualTo(0d);
    assertThat(metricDto.getBestValue()).isEqualTo(100d);
    assertThat(metricDto.isOptimizedBestValue()).isTrue();
    assertThat(metricDto.getDirection()).isEqualTo(1);
    assertThat(metricDto.isEnabled()).isTrue();
  }
}
