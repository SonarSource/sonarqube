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
package org.sonar.api.batch;

import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;

/**
 * @deprecated since 4.2
 */
@Deprecated
public final class SquidUtils {

  private SquidUtils() {
    // only static methods
  }

  /**
   * @deprecated since 4.2 JavaFile is deprecated
   */
  @Deprecated
  public static JavaFile convertJavaFileKeyFromSquidFormat(String key) {
    throw unsupported();
  }

  /**
   * @deprecated since 4.2 JavaPackage is deprecated
   */
  @Deprecated
  public static JavaPackage convertJavaPackageKeyFromSquidFormat(String key) {
    throw unsupported();
  }

  private static UnsupportedOperationException unsupported() {
    return new UnsupportedOperationException("Not supported since v4.2. See http://redirect.sonarsource.com/doc/api-changes.html");
  }

  /**
   * @deprecated since 4.0
   */
  @Deprecated
  public static String convertToSquidKeyFormat(JavaFile file) {
    throw new UnsupportedOperationException("Not supported since v4.0. Was badly implemented");
  }
}
