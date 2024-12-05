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
package org.sonar.core.component;

import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkArgument;

public final class ComponentKeys {

  /**
   * Should be in sync with DefaultIndexedFile.MAX_KEY_LENGTH
   */
  public static final int MAX_COMPONENT_KEY_LENGTH = 999;

  public static final String ALLOWED_CHARACTERS_MESSAGE = "Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit";

  public static final String MALFORMED_KEY_MESSAGE = "Malformed key for '%s'. %s.";

  /**
   * Allowed characters are alphanumeric, '-', '_', '.' and ':'
   */
  private static final String VALID_PROJECT_KEY_CHARS = "\\p{Alnum}-_.:";

  private static final Pattern INVALID_PROJECT_KEY_REGEXP = Pattern.compile("[^" + VALID_PROJECT_KEY_CHARS + "]");

  /**
   * At least one non-digit is necessary
   */
  private static final Pattern VALID_PROJECT_KEY_REGEXP = Pattern.compile("[" + VALID_PROJECT_KEY_CHARS + "]*[\\p{Alpha}\\-_.:]+[" + VALID_PROJECT_KEY_CHARS + "]*");

  private static final String KEY_WITH_BRANCH_FORMAT = "%s:%s";
  private static final String REPLACEMENT_CHARACTER = "_";

  private ComponentKeys() {
    // only static stuff
  }

  public static String createEffectiveKey(String projectKey, @Nullable String path) {
    StringBuilder sb = new StringBuilder(MAX_COMPONENT_KEY_LENGTH);
    sb.append(projectKey);
    if (path != null) {
      sb.append(':').append(path);
    }
    return sb.toString();
  }

  public static boolean isValidProjectKey(String keyCandidate) {
    return VALID_PROJECT_KEY_REGEXP.matcher(keyCandidate).matches();
  }

  /**
   * Checks if given parameter is valid for a project following {@link #isValidProjectKey(String)} contract.
   *
   * @throws IllegalArgumentException if the format is incorrect
   */
  public static void checkProjectKey(String keyCandidate) {
    checkArgument(isValidProjectKey(keyCandidate), MALFORMED_KEY_MESSAGE, keyCandidate, ALLOWED_CHARACTERS_MESSAGE);
  }

  public static String sanitizeProjectKey(String rawProjectKey) {
    return INVALID_PROJECT_KEY_REGEXP.matcher(rawProjectKey).replaceAll(REPLACEMENT_CHARACTER);
  }

  /**
   * Return the project key with potential branch
   */
  public static String createKey(String keyWithoutBranch, @Nullable String branch) {
    if (StringUtils.isNotBlank(branch)) {
      return String.format(KEY_WITH_BRANCH_FORMAT, keyWithoutBranch, branch);
    } else {
      return keyWithoutBranch;
    }
  }

  public static String createKey(String projectKey, @Nullable String path, @Nullable String branch) {
    String key = createKey(projectKey, branch);
    return createEffectiveKey(key, path);
  }
}
