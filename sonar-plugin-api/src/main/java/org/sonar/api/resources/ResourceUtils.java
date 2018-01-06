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

import org.apache.commons.lang.StringUtils;

/**
 * @since 1.10
 * @deprecated since 5.6 as {@link Resource} is deprecated
 */
@Deprecated
public final class ResourceUtils {

  private ResourceUtils() {
  }

  /**
   * @return whether the resource is a view
   */
  public static boolean isView(Resource resource) {
    return isSet(resource) && Qualifiers.VIEW.equals(resource.getQualifier());
  }

  /**
   * @return whether the resource is a subview (in the view tree)
   */
  public static boolean isSubview(Resource resource) {
    return isSet(resource) && Qualifiers.SUBVIEW.equals(resource.getQualifier());
  }

  /**
   * @return whether the resource is the root project
   */
  public static boolean isRootProject(Resource resource) {
    return Qualifiers.PROJECT.equals(resource.getQualifier());
  }

  /**
   * @return whether a resource is a maven module of project
   */
  public static boolean isModuleProject(Resource resource) {
    return Qualifiers.MODULE.equals(resource.getQualifier());
  }

  /**
   * @return whether a resource is a set
   */
  public static boolean isSet(Resource resource) {
    return resource != null && Scopes.PROJECT.equals(resource.getScope());
  }

  /**
   * @return whether a resource is a space
   */
  public static boolean isSpace(Resource resource) {
    return resource != null && Scopes.DIRECTORY.equals(resource.getScope());
  }

  /**
   * @return whether a resource is an entity.
   */
  public static boolean isEntity(Resource resource) {
    return resource != null && Scopes.FILE.equals(resource.getScope());
  }

  /**
   * This method equal isRootProject(resource) or isModuleProject(resource) or isView(resource) or isSubview(resource)
   */
  public static boolean isProject(Resource resource) {
    return isSet(resource);
  }

  /**
   * Alias for {@link #isSpace(Resource)}
   */
  public static boolean isDirectory(Resource resource) {
    return isSpace(resource);
  }

  /**
   * Alias for {@link #isEntity(Resource)}
   */
  public static boolean isFile(Resource resource) {
    return isEntity(resource);
  }

  /* QUALIFIERS */

  /**
   * @return whether a resource is a unit test class
   * @deprecated since 5.1 use {@link #isUnitTestFile(Resource)}
   */
  @Deprecated
  public static boolean isUnitTestClass(Resource resource) {
    return isUnitTestFile(resource);
  }

  /**
   * @return whether a resource is a unit test class
   */
  public static boolean isUnitTestFile(Resource resource) {
    return Qualifiers.UNIT_TEST_FILE.equals(resource.getQualifier());
  }

  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static boolean isLibrary(Resource resource) {
    return Qualifiers.LIBRARY.equals(resource.getQualifier());
  }

  /**
   * @param resource not nullable
   * @return true if this type of resource is persisted in database
   * @since 2.6
   */
  public static boolean isPersistable(Resource resource) {
    return StringUtils.equals(Scopes.PROJECT, resource.getScope()) || StringUtils.equals(Scopes.DIRECTORY, resource.getScope()) ||
      StringUtils.equals(Scopes.FILE, resource.getScope());
  }

}
