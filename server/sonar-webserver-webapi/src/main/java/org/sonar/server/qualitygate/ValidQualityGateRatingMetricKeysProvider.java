/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.qualitygate;

import java.util.HashSet;
import java.util.Set;

/**
 * Extensions can provide additional rating-based metrics that should be allowed to cause
 * quality gate failures. Inject {@link AllowedQualityGateRatingMetricKeysSource} Beans into the ServerSide
 * Spring container context and metric keys provided by those instances will be
 * treated as valid rating metrics.
 */
public class ValidQualityGateRatingMetricKeysProvider {
  private final Set<String> validRatingMetrics = new HashSet<>();

  public ValidQualityGateRatingMetricKeysProvider(Set<AllowedQualityGateRatingMetricKeysSource> allowedQualityGateRatingMetricKeysSources) {
    for (var validRatingMetricSource : allowedQualityGateRatingMetricKeysSources) {
      this.validRatingMetrics.addAll(validRatingMetricSource.metricKeys());
    }
  }

  public boolean isValidRatingMetricKey(String ratingMetricKey) {
    return this.validRatingMetrics.contains(ratingMetricKey);
  }
}
