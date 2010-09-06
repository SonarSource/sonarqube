/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

/**
 * @since 1.10
 */
public final class ResourceUtils {

  private ResourceUtils() {
  }

  /**
   * @return whether the resource is a view
   */
  public static boolean isView(Resource resource) {
    return isSet(resource) && Resource.QUALIFIER_VIEW.equals(resource.getQualifier());
  }

  /**
   * @return whether the resource is a subview (in the view tree)
   */
  public static boolean isSubview(Resource resource) {
    return isSet(resource) && Resource.QUALIFIER_SUBVIEW.equals(resource.getQualifier());
  }

  /**
   * @return whether the resource is the root project
   */
  public static boolean isRootProject(Resource resource) {
    return Resource.QUALIFIER_PROJECT.equals(resource.getQualifier());
  }

  /**
   * @return whther a resource is a maven module of  project
   */
  public static boolean isModuleProject(Resource resource) {
    return Resource.QUALIFIER_MODULE.equals(resource.getQualifier());
  }

  /**
   * @return whether a resource is a package
   */
  public static boolean isPackage(Resource resource) {
    return resource != null && Resource.QUALIFIER_PACKAGE.equals(resource.getQualifier());
  }


  /**
   * @return whether a resource is a set
   */
  public static boolean isSet(Resource resource) {
    return resource != null && Resource.SCOPE_SET.equals(resource.getScope());
  }

  /**
   * @return whether a resource is a space
   */
  public static boolean isSpace(Resource resource) {
    return resource != null && Resource.SCOPE_SPACE.equals(resource.getScope());
  }

  /**
   * @return whether a resource is an entity.
   */
  public static boolean isEntity(Resource resource) {
    return resource != null && Resource.SCOPE_ENTITY.equals(resource.getScope());
  }

  /**
   * This method equal isRootProject(resource) or isModuleProject(resource)
   */
  public static boolean isProject(Resource resource) {
    return isSet(resource);
  }

  /**
   * Alias for isDirectory(resource)
   */
  public static boolean isDirectory(Resource resource) {
    return isSpace(resource);
  }

  /**
   * Alias for isEntity(resource)
   */
  public static boolean isFile(Resource resource) {
    return isEntity(resource);
  }


  /* QUALIFIERS */

  /**
   * @return whether a resource is a class
   */
  public static boolean isClass(Resource resource) {
    return Resource.QUALIFIER_CLASS.equals(resource.getQualifier());
  }


  /**
   * @return whether a resource is a unit test class
   */
  public static boolean isUnitTestClass(Resource resource) {
    return Resource.QUALIFIER_UNIT_TEST_CLASS.equals(resource.getQualifier());
  }


  public static boolean isLibrary(Resource resource) {
    return Resource.QUALIFIER_LIB.equals(resource.getQualifier());
  }
}
