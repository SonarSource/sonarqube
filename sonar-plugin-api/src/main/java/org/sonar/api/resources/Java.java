/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Java language implementation
 *
 * @since 1.10
 */
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
  public String[] getFileSuffixes() {
    return SUFFIXES;
  }

  public static boolean isJavaFile(java.io.File file) {
    String suffix = "." + StringUtils.substringAfterLast(file.getName(), ".");
    return ArrayUtils.contains(SUFFIXES, suffix);
  }

}
