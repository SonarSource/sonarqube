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

import org.apache.commons.lang.StringUtils;

/**
 * Resource scopes are used to group some types of resources. For example Java methods, Flex methods, C functions
 * and Cobol paragraphs are grouped in the scope "block unit".
 * 
 * Scopes are generally used in UI to display/hide some services or in web services.
 *
 * Scopes are not extensible by plugins.
 *
 * @since 2.6
 */
public final class Scopes {

  private Scopes() {
    // only static methods
  }
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
   */
  public static final String TYPE = "TYP";

  /**
   * Block units like methods, functions or Cobol paragraphs.
   */
  public static final String BLOCK_UNIT = "BLU";


  public static boolean isProject(final Resource resource) {
    return resource!=null && StringUtils.equals(PROJECT, resource.getScope());
  }

  public static boolean isDirectory(final Resource resource) {
    return resource!=null && StringUtils.equals(DIRECTORY, resource.getScope());
  }

  public static boolean isFile(final Resource resource) {
    return resource!=null && StringUtils.equals(FILE, resource.getScope());
  }

  public static boolean isType(final Resource resource) {
    return resource!=null && StringUtils.equals(TYPE, resource.getScope());
  }

  public static boolean isBlockUnit(final Resource resource) {
    return resource!=null && StringUtils.equals(BLOCK_UNIT, resource.getScope());
  }
}