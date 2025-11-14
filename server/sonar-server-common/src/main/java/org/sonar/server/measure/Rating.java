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
package org.sonar.server.measure;

import java.util.Map;
import org.sonar.api.issue.impact.Severity;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;

public enum Rating {
  E(5),
  D(4),
  C(3),
  B(2),
  A(1);

  public static final Map<String, Rating> RATING_BY_SEVERITY = Map.of(
    BLOCKER, E,
    CRITICAL, D,
    MAJOR, C,
    MINOR, B,
    INFO, A);

  public static final Map<Severity, Rating> RATING_BY_SOFTWARE_QUALITY_SEVERITY = Map.of(
    Severity.BLOCKER, Rating.E,
    Severity.HIGH, Rating.D,
    Severity.MEDIUM, Rating.C,
    Severity.LOW, Rating.B,
    Severity.INFO, Rating.A);

  private final int index;

  Rating(int index) {
    this.index = index;
  }

  public int getIndex() {
    return index;
  }

  public static Rating valueOf(int index) {
    return stream(Rating.values())
      .filter(r -> r.getIndex() == index)
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException(format("Unknown value '%s'", index)));
  }

  public static Rating ratingFromValue(String value) {
    return valueOf(Integer.parseInt(value));
  }
}
