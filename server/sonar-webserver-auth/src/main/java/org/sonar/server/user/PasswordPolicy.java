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
package org.sonar.server.user;

import java.util.Optional;
import java.util.regex.Pattern;

public final class PasswordPolicy {

  private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
  private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
  private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
  private static final Pattern SPECIAL_CHARACTER_PATTERN = Pattern.compile("[^a-zA-Z0-9]");

  private static final int MIN_PASSWORD_LENGTH = 12;

  public static final String REQUIREMENTS_DESCRIPTION = "The password needs to fulfill the following requirements: at least " + MIN_PASSWORD_LENGTH + " characters "
    + "and contain at least one uppercase character, one lowercase character, one digit and one special character.";

  private PasswordPolicy() {
    // static only
  }

  /**
   * @return the first violated rule of the password policy, or empty if the password complies with it
   */
  public static Optional<String> findViolation(String password) {
    if (password.length() < MIN_PASSWORD_LENGTH) {
      return Optional.of("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
    }
    if (!UPPERCASE_PATTERN.matcher(password).find()) {
      return Optional.of("Password must contain at least one uppercase character");
    }
    if (!LOWERCASE_PATTERN.matcher(password).find()) {
      return Optional.of("Password must contain at least one lowercase character");
    }
    if (!DIGIT_PATTERN.matcher(password).find()) {
      return Optional.of("Password must contain at least one digit");
    }
    if (!SPECIAL_CHARACTER_PATTERN.matcher(password).find()) {
      return Optional.of("Password must contain at least one special character");
    }
    return Optional.empty();
  }
}
