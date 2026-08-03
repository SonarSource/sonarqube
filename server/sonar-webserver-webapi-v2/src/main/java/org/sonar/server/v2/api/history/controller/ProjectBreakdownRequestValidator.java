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

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.lang.Nullable;

final class ProjectBreakdownRequestValidator {

  private ProjectBreakdownRequestValidator() {
  }

  static void validateReferenceDate(Clock clock, @Nullable OffsetDateTime referenceDate) {
    if (referenceDate == null) {
      return;
    }
    Instant referenceInstant = referenceDate.toInstant();
    Instant utcMidnight = clock.instant().truncatedTo(ChronoUnit.DAYS);
    if (!referenceInstant.isBefore(utcMidnight)) {
      throw new IllegalArgumentException("referenceDate %s must be before the current date".formatted(referenceInstant));
    }
    if (referenceInstant.isBefore(utcMidnight.minus(365, ChronoUnit.DAYS))) {
      throw new IllegalArgumentException("referenceDate %s cannot be more than 1 year in the past".formatted(referenceInstant));
    }
  }

  // Invalid selector combinations intentionally raise a client error.
  @SuppressWarnings("java:S6416")
  static void validateSelector(
    @Nullable String portfolioId,
    @Nullable String entityType,
    @Nullable String entityId) {
    if (portfolioId != null && (entityType != null || entityId != null)) {
      throw new IllegalArgumentException("portfolioId cannot be combined with entityType or entityId");
    }
    if (portfolioId == null && (entityType == null || entityId == null)) {
      throw new IllegalArgumentException("Either portfolioId or both entityType and entityId must be provided");
    }
  }

  static @Nullable Instant toInstant(@Nullable OffsetDateTime dateTime) {
    return dateTime == null ? null : dateTime.toInstant();
  }
}
