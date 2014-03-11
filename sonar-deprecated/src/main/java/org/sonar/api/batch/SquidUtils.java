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
package org.sonar.api.batch;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;

public final class SquidUtils {

  private SquidUtils() {
    // only static methods
  }

  /**
   * @deprecated since 4.2 JavaFile is deprecated
   */
  @Deprecated
  public static JavaFile convertJavaFileKeyFromSquidFormat(String key) {
    String extension = StringUtils.lowerCase(FilenameUtils.getExtension(key));
    boolean isJavaFile = "jav".equals(extension) || "java".equals(extension);
    if (isJavaFile) {
      key = key.substring(0, key.length() - extension.length() - 1);
    }

    String convertedKey = key.replace('/', '.');
    if (convertedKey.indexOf('.') == -1 && !"".equals(convertedKey)) {
      convertedKey = "[default]." + convertedKey;

    } else if (convertedKey.indexOf('.') == -1) {
      convertedKey = "[default]";
    }

    return new JavaFile(convertedKey);
  }

  /**
   * @deprecated since 4.2 JavaPackage is deprecated
   */
  @Deprecated
  public static JavaPackage convertJavaPackageKeyFromSquidFormat(String key) {
    return new JavaPackage(key);
  }

  public static String convertToSquidKeyFormat(JavaFile file) {
    throw new UnsupportedOperationException("Not supported since v4.0. Was badly implemented");
  }
}
