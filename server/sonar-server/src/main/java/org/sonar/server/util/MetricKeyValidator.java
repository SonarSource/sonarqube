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
package org.sonar.server.util;

public class MetricKeyValidator {
  /*
   * Allowed characters are alphanumeric, '-', '_', with at least one non-digit
   */
  private static final String VALID_METRIC_KEY_REGEXP = "[\\p{Alnum}\\-_]*[\\p{Alpha}\\-_]+[\\p{Alnum}\\-_]*";

  private MetricKeyValidator() {
    // static stuff only
  }

  /**
   * <p>Test if given parameter is valid for a project/module. Valid format is:</p>
   * <ul>
   *  <li>Allowed characters:
   *    <ul>
   *      <li>Uppercase ASCII letters A-Z</li>
   *      <li>Lowercase ASCII letters a-z</li>
   *      <li>ASCII digits 0-9</li>
   *      <li>Punctuation signs dash '-', underscore '_'</li>
   *    </ul>
   *  </li>
   *  <li>At least one non-digit</li>
   * </ul>
   * @param candidateKey
   * @return <code>true</code> if <code>candidateKey</code> can be used for a metric
   */
  public static boolean isMetricKeyValid(String candidateKey) {
    return candidateKey.matches(VALID_METRIC_KEY_REGEXP);
  }

  public static String checkMetricKeyFormat(String candidateKey) {
    if (!isMetricKeyValid(candidateKey)) {
      throw new IllegalArgumentException(String.format("Malformed metric key '%s'. Allowed characters are alphanumeric, '-', '_', with at least one non-digit.",
        candidateKey));
    }

    return candidateKey;
  }
}
