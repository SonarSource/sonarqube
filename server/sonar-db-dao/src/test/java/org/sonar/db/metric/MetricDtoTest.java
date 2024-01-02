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
package org.sonar.db.metric;

import org.junit.Test;

import static com.google.common.base.Strings.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MetricDtoTest {

  private MetricDto underTest = new MetricDto();

  @Test
  public void getters_and_setters() {
    MetricDto metricDto = new MetricDto()
      .setUuid("1")
      .setKey("coverage")
      .setShortName("Coverage")
      .setDescription("Coverage by unit tests")
      .setDomain("Tests")
      .setValueType("PERCENT")
      .setQualitative(true)
      .setWorstValue(0d)
      .setBestValue(100d)
      .setOptimizedBestValue(true)
      .setDirection(1)
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .setEnabled(true);

    assertThat(metricDto.getUuid()).isEqualTo("1");
    assertThat(metricDto.getKey()).isEqualTo("coverage");
    assertThat(metricDto.getShortName()).isEqualTo("Coverage");
    assertThat(metricDto.getDescription()).isEqualTo("Coverage by unit tests");
    assertThat(metricDto.getDomain()).isEqualTo("Tests");
    assertThat(metricDto.getValueType()).isEqualTo("PERCENT");
    assertThat(metricDto.isQualitative()).isTrue();
    assertThat(metricDto.getWorstValue()).isEqualTo(0d);
    assertThat(metricDto.getBestValue()).isEqualTo(100d);
    assertThat(metricDto.isOptimizedBestValue()).isTrue();
    assertThat(metricDto.getDirection()).isOne();
    assertThat(metricDto.isHidden()).isTrue();
    assertThat(metricDto.isDeleteHistoricalData()).isTrue();
    assertThat(metricDto.isEnabled()).isTrue();
  }

  @Test
  public void fail_if_key_longer_than_64_characters() {
    String a65 = repeat("a", 65);

    assertThatThrownBy(() -> underTest.setKey(a65))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Metric key length (65) is longer than the maximum authorized (64). '" + a65 + "' was provided.");
  }

  @Test
  public void fail_if_name_longer_than_64_characters() {
    String a65 = repeat("a", 65);

    assertThatThrownBy(() -> underTest.setShortName(a65))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Metric name length (65) is longer than the maximum authorized (64). '" + a65 + "' was provided.");
  }

  @Test
  public void fail_if_description_longer_than_255_characters() {
    String a256 = repeat("a", 256);

    assertThatThrownBy(() -> underTest.setDescription(a256))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Metric description length (256) is longer than the maximum authorized (255). '" + a256 + "' was provided.");
  }

  @Test
  public void fail_if_domain_longer_than_64_characters() {
    String a65 = repeat("a", 65);

    assertThatThrownBy(() -> underTest.setDomain(a65))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Metric domain length (65) is longer than the maximum authorized (64). '" + a65 + "' was provided.");
  }
}
