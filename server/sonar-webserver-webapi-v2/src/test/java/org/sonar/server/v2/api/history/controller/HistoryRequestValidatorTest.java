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
package org.sonar.server.v2.api.history.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REVIEW_RATING;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING;

public class HistoryRequestValidatorTest {

  private static final String HOTSPOT_MESSAGE = "Security Hotspot metric '%s' is not supported by this endpoint";
  private final HistoryRequestValidator underTest = new HistoryRequestValidator();

  @Test
  public void rejectsEachSecurityHotspotMetricKey() {
    List<String> hotspotMetricKeys = List.of(
      SECURITY_HOTSPOTS.getKey(),
      SECURITY_HOTSPOTS_REVIEWED.getKey(),
      NEW_SECURITY_HOTSPOTS.getKey(),
      NEW_SECURITY_HOTSPOTS_REVIEWED.getKey(),
      SECURITY_REVIEW_RATING.getKey(),
      NEW_SECURITY_REVIEW_RATING.getKey());

    hotspotMetricKeys.forEach(metricKey -> {
      List<String> metricKeys = List.of(metricKey);
      assertThatThrownBy(() -> underTest.validateMetricKeys(metricKeys))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(HOTSPOT_MESSAGE.formatted(metricKey));
    });
  }

  @Test
  public void rejectsSecurityHotspotMetricKeysRegardlessOfCase() {
    List<String> hotspotMetricKeys = List.of(
      SECURITY_HOTSPOTS.getKey(),
      SECURITY_HOTSPOTS_REVIEWED.getKey(),
      NEW_SECURITY_HOTSPOTS.getKey(),
      NEW_SECURITY_HOTSPOTS_REVIEWED.getKey(),
      SECURITY_REVIEW_RATING.getKey(),
      NEW_SECURITY_REVIEW_RATING.getKey());

    hotspotMetricKeys.forEach(metricKey -> {
      String upperCaseMetricKey = metricKey.toUpperCase(Locale.ROOT);
      List<String> metricKeys = List.of(upperCaseMetricKey);
      assertThatThrownBy(() -> underTest.validateMetricKeys(metricKeys))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(HOTSPOT_MESSAGE.formatted(upperCaseMetricKey));
    });
  }

  @Test
  public void rejectsHotspotMetricWhenMixedWithRegularMetrics() {
    List<String> metricKeys = List.of("ncloc", "new_security_hotspots", "coverage");
    assertThatThrownBy(() -> underTest.validateMetricKeys(metricKeys))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(HOTSPOT_MESSAGE.formatted("new_security_hotspots"));
  }

  @Test
  public void acceptsRegularMetricKeys() {
    List<String> regularMetricKeys = List.of("ncloc", "coverage");
    List<String> emptyMetricKeys = List.of();
    List<String> metricKeysWithNull = Arrays.asList(null, "coverage");
    assertThatCode(() -> underTest.validateMetricKeys(regularMetricKeys)).doesNotThrowAnyException();
    assertThatCode(() -> underTest.validateMetricKeys(emptyMetricKeys)).doesNotThrowAnyException();
    assertThatCode(() -> underTest.validateMetricKeys(null)).doesNotThrowAnyException();
    assertThatCode(() -> underTest.validateMetricKeys(metricKeysWithNull)).doesNotThrowAnyException();
  }

}
