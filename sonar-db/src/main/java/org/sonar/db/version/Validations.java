/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version;

import com.google.common.base.CharMatcher;
import javax.annotation.Nullable;

import static com.google.common.base.CharMatcher.anyOf;
import static com.google.common.base.CharMatcher.inRange;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

public class Validations {

  static final int TABLE_NAME_MAX_SIZE = 25;
  static final int CONSTRAINT_NAME_MAX_SIZE = 30;

  private static final CharMatcher DIGIT_CHAR_MATCHER = inRange('0', '9');
  private static final CharMatcher LOWER_CASE_ASCII_LETTERS_CHAR_MATCHER = inRange('a', 'z');
  private static final CharMatcher UNDERSCORE_CHAR_MATCHER = anyOf("_");

  private Validations() {
    // Only static stuff here
  }

  static String validateColumnName(@Nullable String columnName) {
    String name = requireNonNull(columnName, "Column name cannot be null");
    checkDbIdentifierCharacters(columnName, "Column name");
    return name;
  }

  /**
   * Ensure {@code identifier} is a valid DB identifier.
   *
   * @throws NullPointerException if {@code identifier} is {@code null}
   * @throws IllegalArgumentException if {@code identifier} is empty
   * @throws IllegalArgumentException if {@code identifier} is longer than {@code maxSize}
   * @throws IllegalArgumentException if {@code identifier} is not lowercase
   * @throws IllegalArgumentException if {@code identifier} contains characters others than ASCII letters, ASCII numbers or {@code _}
   * @throws IllegalArgumentException if {@code identifier} starts with {@code _} or a number
   */
  static String checkDbIdentifier(@Nullable String identifier, String identifierDesc, int maxSize) {
    String res = checkNotNull(identifier, "%s can't be null", identifierDesc);
    checkArgument(!res.isEmpty(), "%s, can't be empty", identifierDesc);
    checkArgument(
      identifier.length() <= maxSize,
      "%s length can't be more than %s", identifierDesc, maxSize);
    checkDbIdentifierCharacters(identifier, identifierDesc);
    return res;
  }

  private static void checkDbIdentifierCharacters(String identifier, String identifierDesc) {
    checkArgument(
      LOWER_CASE_ASCII_LETTERS_CHAR_MATCHER.or(DIGIT_CHAR_MATCHER).or(anyOf("_")).matchesAllOf(identifier),
      "%s must be lower case and contain only alphanumeric chars or '_', got '%s'", identifierDesc, identifier);
    checkArgument(
      DIGIT_CHAR_MATCHER.or(UNDERSCORE_CHAR_MATCHER).matchesNoneOf(identifier.subSequence(0, 1)),
      "%s must not start by a number or '_', got '%s'", identifierDesc, identifier);
  }
}
