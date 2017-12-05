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

import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * The qualifier determines the exact type of a resource.
 * Plugins can define their own qualifiers.
 *
 * @since 2.6
 */
public final class Qualifiers {

  /**
   * Root portfolios. Scope of portfolios is Scopes.PROJECT
   */
  public static final String VIEW = "VW";

  /**
   * Sub-portfolios, defined in root portfolios. Scope of sub-portfolios is Scopes.PROJECT
   */
  public static final String SUBVIEW = "SVW";

  /**
   * Application portfolios. Scope of application is Scopes.PROJECT
   */
  public static final String APP = "APP";

  /**
   * Library, for example a JAR dependency of Java projects.
   * Scope of libraries is Scopes.PROJECT
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final String LIBRARY = "LIB";

  /**
   * Single project or root of multi-modules projects
   * Scope is Scopes.PROJECT
   */
  public static final String PROJECT = "TRK";

  /**
   * Module of a multi-modules project. It's sometimes called "sub-project".
   * Scope of modules is Scopes.PROJECT
   */
  public static final String MODULE = "BRC";

  public static final String DIRECTORY = "DIR";
  public static final String FILE = "FIL";

  // ugly, should be replaced by "natures"
  public static final String UNIT_TEST_FILE = "UTS";

  /**
   * List of qualifiers, ordered from bottom to up regarding position
   * in tree of components
   *
   * @since 7.0
   */
  public static final List<String> ORDERED_BOTTOM_UP = unmodifiableList(asList(
    FILE, UNIT_TEST_FILE, DIRECTORY, MODULE, PROJECT, APP, SUBVIEW, VIEW));

  private Qualifiers() {
    // only static methods
  }

  /**
   * @param resource not nullable
   */
  public static boolean isView(final Resource resource, final boolean acceptSubViews) {
    boolean isView = Objects.equals(VIEW, resource.getQualifier());
    if (!isView && acceptSubViews) {
      isView = Objects.equals(SUBVIEW, resource.getQualifier());
    }

    return isView;
  }

  /**
   * @param resource not nullable
   */
  public static boolean isSubview(final Resource resource) {
    return Objects.equals(SUBVIEW, resource.getScope());
  }

  /**
   * @param resource not nullable
   */
  public static boolean isProject(final Resource resource, final boolean acceptModules) {
    boolean isProject = Objects.equals(PROJECT, resource.getQualifier());
    if (!isProject && acceptModules) {
      isProject = Objects.equals(MODULE, resource.getQualifier());
    }
    return isProject;
  }

  /**
   * @param resource not nullable
   */
  public static boolean isModule(final Resource resource) {
    return Objects.equals(MODULE, resource.getQualifier());
  }

  /**
   * @param resource not nullable
   */
  public static boolean isDirectory(final Resource resource) {
    return Objects.equals(DIRECTORY, resource.getQualifier());
  }

  /**
   * @param resource not nullable
   */
  public static boolean isFile(final Resource resource) {
    return Objects.equals(FILE, resource.getQualifier());
  }
}
