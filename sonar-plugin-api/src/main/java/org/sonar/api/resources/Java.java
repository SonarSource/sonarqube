/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * Java language implementation
 * This class have been moved in the plugin sonar-java
 *
 * @since 1.10
 * @deprecated in 3.6
 */
@Deprecated
public class Java extends AbstractLanguage {

  public static final Java INSTANCE = new Java();

  /**
   * Java key
   */
  public static final String KEY = "java";

  /**
   * Java name
   */
  public static final String NAME = "Java";

  /**
   * Default package name for classes without package def
   */
  public static final String DEFAULT_PACKAGE_NAME = "[default]";

  /**
   * Java files knows suffixes
   */
  public static final String[] SUFFIXES = {".java", ".jav"};

  /**
   * Default constructor
   */
  public Java() {
    super(KEY, NAME);
  }

  /**
   * {@inheritDoc}
   *
   * @see AbstractLanguage#getFileSuffixes()
   */
  @Override
  public String[] getFileSuffixes() {
    return SUFFIXES;
  }

  public static boolean isJavaFile(java.io.File file) {
    String suffix = "." + StringUtils.lowerCase(StringUtils.substringAfterLast(file.getName(), "."));
    return ArrayUtils.contains(SUFFIXES, suffix);
  }

}
