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
package org.sonar.api.resources;

import org.apache.commons.lang.StringUtils;

/**
 * The qualifier determines the exact type of a resource.
 * Plugins can define their own qualifiers.
 *
 * @since 2.6
 */
public final class Qualifiers {

  private Qualifiers() {
    // only static methods
  }

  /**
   * Root views. Scope of views is Scopes.PROJECT
   */
  public static final String VIEW = "VW";

  /**
   * Sub-views, defined in root views. Scope of sub-views is Scopes.PROJECT
   */
  public static final String SUBVIEW = "SVW";

  /**
   * Library, for example a JAR dependency of Java projects.
   * Scope of libraries is Scopes.PROJECT
   */
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

  /**
   * @deprecated since 4.2 now packages are considered as regular directories. Use {@link #DIRECTORY} instead.
   */
  @Deprecated
  public static final String PACKAGE = "PAC";
  public static final String DIRECTORY = "DIR";
  public static final String FILE = "FIL";
  /**
   * @deprecated since 4.2 now java files are considered as regular files. Use {@link #FILE} instead.
   */
  @Deprecated
  public static final String CLASS = "CLA";
  /**
   * @deprecated since 4.2 resources under FILE level will no more be supported.
   */
  @Deprecated
  public static final String PARAGRAPH = "PAR";
  /**
   * @deprecated since 4.2 resources under FILE level will no more be supported.
   */
  @Deprecated
  public static final String METHOD = "MET";
  /**
   * @deprecated since 4.2 resources under FILE level will no more be supported.
   */
  @Deprecated
  public static final String FIELD = "FLD";

  // ugly, should be replaced by "natures"
  public static final String UNIT_TEST_FILE = "UTS";

  /**
   * @param resource not nullable
   */
  public static boolean isView(final Resource resource, final boolean acceptSubViews) {
    boolean isView = StringUtils.equals(VIEW, resource.getQualifier());
    if (!isView && acceptSubViews) {
      isView = StringUtils.equals(SUBVIEW, resource.getQualifier());
    }

    return isView;
  }

  /**
   * @param resource not nullable
   */
  public static boolean isSubview(final Resource resource) {
    return StringUtils.equals(SUBVIEW, resource.getScope());
  }

  /**
   * @param resource not nullable
   */
  public static boolean isProject(final Resource resource, final boolean acceptModules) {
    boolean isProject = StringUtils.equals(PROJECT, resource.getQualifier());
    if (!isProject && acceptModules) {
      isProject = StringUtils.equals(MODULE, resource.getQualifier());
    }
    return isProject;
  }

  /**
   * @param resource not nullable
   */
  public static boolean isModule(final Resource resource) {
    return StringUtils.equals(MODULE, resource.getQualifier());
  }

  /**
   * @param resource not nullable
   */
  public static boolean isDirectory(final Resource resource) {
    return StringUtils.equals(DIRECTORY, resource.getQualifier());
  }

  /**
   * @deprecated since 4.2 Package is now a directory. Use {@link #isDirectory(Resource)}
   */
  @Deprecated
  public static boolean isPackage(final Resource resource) {
    return StringUtils.equals(PACKAGE, resource.getQualifier()) || isDirectory(resource);
  }

  /**
   * @param resource not nullable
   */
  public static boolean isFile(final Resource resource) {
    return StringUtils.equals(FILE, resource.getQualifier());
  }

  /**
   * @param resource not nullable
   * @deprecated since 4.2 CLA qualifier is deprecated
   */
  @Deprecated
  public static boolean isClass(final Resource resource) {
    return StringUtils.equals(CLASS, resource.getQualifier()) || isFile(resource);
  }

  /**
   * @param resource not nullable
   */
  public static boolean isMethod(final Resource resource) {
    return StringUtils.equals(METHOD, resource.getQualifier());
  }

  /**
   * @param resource not nullable
   */
  public static boolean isParagraph(final Resource resource) {
    return StringUtils.equals(PARAGRAPH, resource.getQualifier());
  }
}
