/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;

import static com.google.common.base.Preconditions.checkArgument;

public final class ComponentKeys {

  public static final int MAX_COMPONENT_KEY_LENGTH = 400;

  /*
   * Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit
   */
  private static final String VALID_MODULE_KEY_REGEXP = "[\\p{Alnum}\\-_.:]*[\\p{Alpha}\\-_.:]+[\\p{Alnum}\\-_.:]*";

  private static final String VALID_MODULE_KEY_ISSUES_MODE_REGEXP = "[\\p{Alnum}\\-_.:/]*[\\p{Alpha}\\-_.:/]+[\\p{Alnum}\\-_.:/]*";
  /*
   * Allowed characters are alphanumeric, '-', '_', '.' and '/'
   */
  private static final String VALID_BRANCH_REGEXP = "[\\p{Alnum}\\-_./]*";

  private static final String KEY_WITH_BRANCH_FORMAT = "%s:%s";

  private ComponentKeys() {
    // only static stuff
  }

  /**
   * @return the full key of a component, based on its parent projects' key and own key
   */
  public static String createEffectiveKey(String moduleKey, Resource resource) {
    if (!StringUtils.equals(Scopes.PROJECT, resource.getScope())) {
      // not a project nor a library
      return new StringBuilder(MAX_COMPONENT_KEY_LENGTH)
        .append(moduleKey)
        .append(':')
        .append(resource.getKey())
        .toString();
    }
    return resource.getKey();
  }

  public static String createEffectiveKey(String moduleKey, InputPath inputPath) {
    return createEffectiveKey(moduleKey, inputPath.relativePath());
  }

  public static String createEffectiveKey(String moduleKey, @Nullable String path) {
    StringBuilder sb = new StringBuilder(MAX_COMPONENT_KEY_LENGTH);
    sb.append(moduleKey);
    if (path != null) {
      sb.append(':').append(path);
    }
    return sb.toString();
  }

  /**
   * <p>Test if given parameter is valid for a project/module. Valid format is:</p>
   * <ul>
   *  <li>Allowed characters:
   *    <ul>
   *      <li>Uppercase ASCII letters A-Z</li>
   *      <li>Lowercase ASCII letters a-z</li>
   *      <li>ASCII digits 0-9</li>
   *      <li>Punctuation signs dash '-', underscore '_', period '.' and colon ':'</li>
   *    </ul>
   *  </li>
   *  <li>At least one non-digit</li>
   * </ul>
   * @param keyCandidate
   * @return <code>true</code> if <code>keyCandidate</code> can be used for a project/module
   */
  public static boolean isValidModuleKey(String keyCandidate) {
    return keyCandidate.matches(VALID_MODULE_KEY_REGEXP);
  }

  /**
   * Checks if given parameter is valid for a project/module following {@link #isValidModuleKey(String)} contract.
   *
   * @throws IllegalArgumentException if the format is incorrect
   */
  public static void checkModuleKey(String keyCandidate) {
    checkArgument(isValidModuleKey(keyCandidate), "Malformed key for '%s'. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.", keyCandidate);
  }

  /**
   * Same as {@link #isValidModuleKey(String)}, but allows additionally '/'.
   */
  public static boolean isValidModuleKeyIssuesMode(String keyCandidate) {
    return keyCandidate.matches(VALID_MODULE_KEY_ISSUES_MODE_REGEXP);
  }

  /**
   * <p>Test if given parameter is valid for a branch. Valid format is:</p>
   * <ul>
   *  <li>Allowed characters:
   *    <ul>
   *      <li>Uppercase ASCII letters A-Z</li>
   *      <li>Lowercase ASCII letters a-z</li>
   *      <li>ASCII digits 0-9</li>
   *      <li>Punctuation signs dash '-', underscore '_', period '.', and '/'</li>
   *    </ul>
   *  </li>
   * </ul>
   * @param branchCandidate
   * @return <code>true</code> if <code>branchCandidate</code> can be used for a project/module
   */
  public static boolean isValidBranch(String branchCandidate) {
    return branchCandidate.matches(VALID_BRANCH_REGEXP);
  }

  /**
   * Return the project/module key with potential branch
   * @param keyWithoutBranch
   * @param branch
   * @return
   */
  public static String createKey(String keyWithoutBranch, @Nullable String branch) {
    if (StringUtils.isNotBlank(branch)) {
      return String.format(KEY_WITH_BRANCH_FORMAT, keyWithoutBranch, branch);
    } else {
      return keyWithoutBranch;
    }
  }

  public static String createKey(String moduleKey, @Nullable String path, @Nullable String branch) {
    String key = createKey(moduleKey, branch);
    return createEffectiveKey(key, path);
  }
}
