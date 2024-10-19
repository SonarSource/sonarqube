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
package org.sonar.db.component;

import org.junit.jupiter.api.Test;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisPropertyDtoTest {

  private AnalysisPropertyDto underTest;

  @Test
  void null_key_should_throw_NPE() {
    underTest = new AnalysisPropertyDto();

    assertThatThrownBy(() -> underTest.setKey(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("key cannot be null");
  }

  @Test
  void null_value_should_throw_NPE() {
    underTest = new AnalysisPropertyDto();

    assertThatThrownBy(() -> underTest.setValue(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("value cannot be null");
  }

  @Test
  void null_uuid_should_throw_NPE() {
    underTest = new AnalysisPropertyDto();

    assertThatThrownBy(() -> underTest.setUuid(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("uuid cannot be null");
  }

  @Test
  void null_analysis_uuid_should_throw_NPE() {
    underTest = new AnalysisPropertyDto();

    assertThatThrownBy(() -> underTest.setAnalysisUuid(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("analysisUuid cannot be null");
  }

  @Test
  void test_equality() {
    underTest = new AnalysisPropertyDto()
      .setUuid(randomAlphanumeric(40))
      .setAnalysisUuid(randomAlphanumeric(40))
      .setKey(randomAlphanumeric(512))
      .setValue(randomAlphanumeric(10000));

    assertThat(underTest)
      .isEqualTo(
        new AnalysisPropertyDto()
          .setUuid(underTest.getUuid())
          .setAnalysisUuid(underTest.getAnalysisUuid())
          .setKey(underTest.getKey())
          .setValue(underTest.getValue()))
      .isNotEqualTo(
        new AnalysisPropertyDto()
          .setUuid("1" + underTest.getUuid())
          .setAnalysisUuid(underTest.getAnalysisUuid())
          .setKey(underTest.getKey())
          .setValue(underTest.getValue()))
      .isNotEqualTo(
        new AnalysisPropertyDto()
          .setUuid(underTest.getUuid())
          .setAnalysisUuid("1" + underTest.getAnalysisUuid())
          .setKey(underTest.getKey())
          .setValue(underTest.getValue()))
      .isNotEqualTo(
        new AnalysisPropertyDto()
          .setUuid(underTest.getUuid())
          .setAnalysisUuid(underTest.getAnalysisUuid())
          .setKey("1" + underTest.getKey())
          .setValue(underTest.getValue()))
      .isNotEqualTo(
        new AnalysisPropertyDto()
          .setUuid(underTest.getUuid())
          .setAnalysisUuid(underTest.getAnalysisUuid())
          .setKey(underTest.getKey())
          .setValue("1" + underTest.getValue()));
  }

  @Test
  void test_hashcode() {
    underTest = new AnalysisPropertyDto()
      .setUuid(randomAlphanumeric(40))
      .setAnalysisUuid(randomAlphanumeric(40))
      .setKey(randomAlphanumeric(512))
      .setValue(randomAlphanumeric(10000));

    assertThat(underTest.hashCode()).isEqualTo(
      new AnalysisPropertyDto()
        .setUuid(underTest.getUuid())
        .setAnalysisUuid(underTest.getAnalysisUuid())
        .setKey(underTest.getKey())
        .setValue(underTest.getValue())
        .hashCode());

    assertThat(underTest.hashCode()).isNotEqualTo(
      new AnalysisPropertyDto()
        .setUuid("1" + underTest.getUuid())
        .setAnalysisUuid(underTest.getAnalysisUuid())
        .setKey(underTest.getKey())
        .setValue(underTest.getValue())
        .hashCode());

    assertThat(underTest.hashCode()).isNotEqualTo(
      new AnalysisPropertyDto()
        .setUuid(underTest.getUuid())
        .setAnalysisUuid("1" + underTest.getAnalysisUuid())
        .setKey(underTest.getKey())
        .setValue(underTest.getValue())
        .hashCode());

    assertThat(underTest.hashCode()).isNotEqualTo(
      new AnalysisPropertyDto()
        .setUuid(underTest.getUuid())
        .setAnalysisUuid(underTest.getAnalysisUuid())
        .setKey("1" + underTest.getKey())
        .setValue(underTest.getValue())
        .hashCode());

    assertThat(underTest.hashCode()).isNotEqualTo(
      new AnalysisPropertyDto()
        .setUuid(underTest.getUuid())
        .setAnalysisUuid(underTest.getAnalysisUuid())
        .setKey(underTest.getKey())
        .setValue("1" + underTest.getValue())
        .hashCode());
  }
}
