/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;

public final class SquidUtils {

  private static final String JAVA_FILE_SUFFIX = ".java";

  private SquidUtils() {
    // only static methods
  }

  public static JavaFile convertJavaFileKeyFromSquidFormat(String key) {
    boolean isJavaFile = key.endsWith(JAVA_FILE_SUFFIX);
    if (isJavaFile) {
      key = key.substring(0, key.length() - JAVA_FILE_SUFFIX.length());
    }

    String convertedKey = key.replace('/', '.');
    if (convertedKey.indexOf('.') == -1 && !"".equals(convertedKey)) {
      convertedKey = "[default]." + convertedKey;

    } else if (convertedKey.indexOf('.') == -1) {
      convertedKey = "[default]";
    }

    return new JavaFile(convertedKey);
  }

  public static JavaPackage convertJavaPackageKeyFromSquidFormat(String key) {
    String convertedKey = key.replace('/', '.');
    return new JavaPackage(convertedKey);
  }

  public static String convertToSquidKeyFormat(JavaFile file) {
    String key = file.getKey();
    if (file.getParent() == null || file.getParent().isDefault()) {
      key = StringUtils.substringAfterLast(file.getKey(), ".");
    } else {
      key = StringUtils.replace(key, ".", "/");
    }
    return key + JAVA_FILE_SUFFIX;
  }
}
