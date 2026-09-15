/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.qualityprofile;

import java.util.Date;
import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QPMeasureDataTest {

  private static final Date RULES_UPDATED_AT = new Date(1_704_067_200_000L);

  @Test
  public void containsLanguage_returns_true_when_language_present() {
    String json = QPMeasureData.toJson(new QPMeasureData(List.of(
      new QualityProfile("java-key", "Sonar way", "java", RULES_UPDATED_AT),
      new QualityProfile("secrets-key", "Sonar way", "secrets", RULES_UPDATED_AT))));

    assertThat(QPMeasureData.containsLanguage(json, "secrets")).isTrue();
  }

  @Test
  public void containsLanguage_returns_false_when_language_absent() {
    String json = QPMeasureData.toJson(new QPMeasureData(List.of(
      new QualityProfile("java-key", "Sonar way", "java", RULES_UPDATED_AT))));

    assertThat(QPMeasureData.containsLanguage(json, "secrets")).isFalse();
  }

  @Test
  public void containsLanguage_returns_false_when_json_array_is_empty() {
    String json = QPMeasureData.toJson(new QPMeasureData(List.of()));

    assertThat(QPMeasureData.containsLanguage(json, "secrets")).isFalse();
  }

  @Test
  public void containsLanguage_returns_false_when_language_argument_is_null() {
    String json = QPMeasureData.toJson(new QPMeasureData(List.of(
      new QualityProfile("java-key", "Sonar way", "java", RULES_UPDATED_AT))));

    assertThat(QPMeasureData.containsLanguage(json, null)).isFalse();
  }

  @Test
  public void fromJson_and_toJson_round_trip() {
    QualityProfile javaProfile = new QualityProfile("java-key", "Sonar way", "java", RULES_UPDATED_AT);
    String json = QPMeasureData.toJson(new QPMeasureData(List.of(javaProfile)));

    assertThat(QPMeasureData.fromJson(json).getProfiles()).containsExactly(javaProfile);
  }
}
