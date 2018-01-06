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
package org.sonar.api.resources;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Resource scopes are used to group some types of resources. For example Java methods, Flex methods, C functions
 * and Cobol paragraphs are grouped in the scope "block unit".
 * <br>
 * Scopes are generally used in UI to display/hide some services or in web services.
 * <br>
 * Scopes are not extensible by plugins.
 *
 * @since 2.6
 */
public final class Scopes {

  /**
   * For example view, subview, project, module or library. Persisted in database.
   */
  public static final String PROJECT = "PRJ";

  /**
   * For example directory or Java package. Persisted in database. A more generic term for this scope could
   * be "namespace"
   */
  public static final String DIRECTORY = "DIR";

  /**
   * For example a Java file. Persisted in database. A more generic term for this scope could
   * be "compilation unit". It's the lowest scope in file system units.
   */
  public static final String FILE = "FIL";

  /**
   * Types like Java classes/interfaces. Not persisted in database.
   * @deprecated since 4.3 resources under FILE level are no more be supported since 4.2.
   */
  @Deprecated
  public static final String PROGRAM_UNIT = "PGU";

  /**
   * Block units like methods, functions or Cobol paragraphs.
   * @deprecated since 4.3 resources under FILE level are no more be supported since 4.2.
   */
  @Deprecated
  public static final String BLOCK_UNIT = "BLU";

  public static final String[] SORTED_SCOPES = {PROJECT, DIRECTORY, FILE, PROGRAM_UNIT, BLOCK_UNIT};

  private Scopes() {
    // only static methods
  }

  public static boolean isProject(final Resource resource) {
    return StringUtils.equals(PROJECT, resource.getScope());
  }

  public static boolean isDirectory(final Resource resource) {
    return StringUtils.equals(DIRECTORY, resource.getScope());
  }

  /**
   * This scope is sometimes called a "compilation unit".
   */
  public static boolean isFile(final Resource resource) {
    return StringUtils.equals(FILE, resource.getScope());
  }

  /**
   * A program unit can be a Java class.
   * @deprecated since 4.3 resources under FILE level are no more be supported since 4.2.
   */
  @Deprecated
  public static boolean isProgramUnit(final Resource resource) {
    return StringUtils.equals(PROGRAM_UNIT, resource.getScope());
  }

  /**
   * @deprecated since 4.3 resources under FILE level are no more be supported since 4.2.
   */
  @Deprecated
  public static boolean isBlockUnit(final Resource resource) {
    return StringUtils.equals(BLOCK_UNIT, resource.getScope());
  }

  public static boolean isHigherThan(final Resource resource, final String than) {
    return isHigherThan(resource.getScope(), than);
  }

  public static boolean isHigherThan(final String scope, final String than) {
    int index = ArrayUtils.indexOf(SORTED_SCOPES, scope);
    int thanIndex = ArrayUtils.indexOf(SORTED_SCOPES, than);
    return index < thanIndex;
  }

  public static boolean isHigherThanOrEquals(final Resource resource, final String than) {
    return isHigherThanOrEquals(resource.getScope(), than);
  }

  public static boolean isHigherThanOrEquals(final String scope, final String than) {
    int index = ArrayUtils.indexOf(SORTED_SCOPES, scope);
    int thanIndex = ArrayUtils.indexOf(SORTED_SCOPES, than);
    return index <= thanIndex;
  }
}
