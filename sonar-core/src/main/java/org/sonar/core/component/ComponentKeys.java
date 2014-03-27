/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.component;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;

import javax.annotation.Nullable;

public final class ComponentKeys {

  /*
   * Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit
   */
  private static final String VALID_MODULE_KEY_REGEXP = "[\\p{Alnum}\\-_.:]*[\\p{Alpha}\\-_.:]+[\\p{Alnum}\\-_.:]*";
  /*
   * Allowed characters are alphanumeric, '-', '_' and '.'
   */
  private static final String VALID_BRANCH_REGEXP = "[\\p{Alnum}\\-_.]*";

  private static final String KEY_WITH_BRANCH_FORMAT = "%s:%s";

  private ComponentKeys() {
    // only static stuff
  }

  /**
   *
   * @param project
   * @param resource
   * @return the full key of a component, based on its parent projects' key and own key
   */
  public static String createEffectiveKey(Project project, Resource resource) {
    String key = resource.getKey();
    if (!StringUtils.equals(Scopes.PROJECT, resource.getScope())) {
      // not a project nor a library
      key = new StringBuilder(ResourceModel.KEY_SIZE)
        .append(project.getKey())
        .append(':')
        .append(resource.getKey())
        .toString();
    }
    return key;
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
   * <p>Test if given parameter is valid for a branch. Valid format is:</p>
   * <ul>
   *  <li>Allowed characters:
   *    <ul>
   *      <li>Uppercase ASCII letters A-Z</li>
   *      <li>Lowercase ASCII letters a-z</li>
   *      <li>ASCII digits 0-9</li>
   *      <li>Punctuation signs dash '-', underscore '_', and period '.'</li>
   *    </ul>
   *  </li>
   * </ul>
   * @param keyCandidate
   * @return <code>true</code> if <code>keyCandidate</code> can be used for a project/module
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
}
