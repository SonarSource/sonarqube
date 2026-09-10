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

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REVIEW_RATING;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING;

/** Validates restrictions specific to the server's history dashboard APIs. */
@Component
public class HistoryRequestValidator {

  private static final Set<String> SECURITY_HOTSPOT_METRIC_KEYS = Set.of(
    SECURITY_HOTSPOTS.getKey(),
    SECURITY_HOTSPOTS_REVIEWED.getKey(),
    NEW_SECURITY_HOTSPOTS.getKey(),
    NEW_SECURITY_HOTSPOTS_REVIEWED.getKey(),
    SECURITY_REVIEW_RATING.getKey(),
    NEW_SECURITY_REVIEW_RATING.getKey());

  public void validateMetricKeys(@Nullable Collection<String> metricKeys) {
    if (metricKeys == null) {
      return;
    }
    for (String metricKey : metricKeys) {
      if (metricKey != null && SECURITY_HOTSPOT_METRIC_KEYS.contains(metricKey.toLowerCase(Locale.ROOT))) {
        throw new IllegalArgumentException("Security Hotspot metric '%s' is not supported by this endpoint".formatted(metricKey));
      }
    }
  }
}
