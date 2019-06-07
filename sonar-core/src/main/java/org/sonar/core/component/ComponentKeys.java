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
package org.sonar.core.component;

import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

import static com.google.common.base.Preconditions.checkArgument;

public final class ComponentKeys {

  public static final int MAX_COMPONENT_KEY_LENGTH = 400;

  /*
   * Must not be blank or empty
   */
  private static final String VALID_PROJECT_KEY_REGEXP = "[^\\p{javaWhitespace}]+";

  /*
   * Allowed characters are alphanumeric, '-', '_', '.' and '/'
   */
  private static final String VALID_BRANCH_REGEXP = "[\\p{Alnum}\\-_./]*";

  private static final String KEY_WITH_BRANCH_FORMAT = "%s:%s";

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

  /**
   * Test if given parameter is valid for a project. A key is valid if it doesn't contain whitespaces.
   *
   * @return <code>true</code> if <code>keyCandidate</code> can be used for a project
   */
  public static boolean isValidProjectKey(String keyCandidate) {
    return keyCandidate.matches(VALID_PROJECT_KEY_REGEXP);
  }

  /**
   * Checks if given parameter is valid for a project following {@link #isValidProjectKey(String)} contract.
   *
   * @throws IllegalArgumentException if the format is incorrect
   */
  public static void checkProjectKey(String keyCandidate) {
    checkArgument(isValidProjectKey(keyCandidate), "Malformed key for '%s'. %s", keyCandidate, "Project key cannot be empty nor contain whitespaces.");
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
