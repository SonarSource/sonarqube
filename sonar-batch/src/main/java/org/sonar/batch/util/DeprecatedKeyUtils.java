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
package org.sonar.batch.util;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.JavaPackage;

public class DeprecatedKeyUtils {

  private DeprecatedKeyUtils() {
    // Utility class
  }

  /**
   * Return the parent directory deprecated key for a given deprecated Java file key.
   * "com.foo.Bar" -> "com/foo"
   * "[root].Bar" -> "[root]"
   * "Bar" -> "[root]"
   */
  public static String getJavaFileParentDeprecatedKey(String deprecatedJavaFileKey) {
    String packageFullyQualifiedName;
    String realKey = StringUtils.trim(deprecatedJavaFileKey);
    if (realKey.contains(".")) {
      packageFullyQualifiedName = StringUtils.substringBeforeLast(realKey, ".");
      String deprecatedDirectoryKey = StringUtils.trimToEmpty(packageFullyQualifiedName);
      if (JavaPackage.DEFAULT_PACKAGE_NAME.equals(deprecatedDirectoryKey)) {
        return Directory.ROOT;
      }
      deprecatedDirectoryKey = deprecatedDirectoryKey.replaceAll("\\.", Directory.SEPARATOR);
      return StringUtils.defaultIfEmpty(deprecatedDirectoryKey, Directory.ROOT);
    } else {
      return Directory.ROOT;
    }
  }

  /**
   * Return the deprecated key of a Java file given its path relative to source directory.
   */
  public static String getJavaFileDeprecatedKey(String sourceRelativePath) {
    String pacname = null;
    String classname = sourceRelativePath;

    if (sourceRelativePath.indexOf('/') >= 0) {
      pacname = StringUtils.substringBeforeLast(sourceRelativePath, "/");
      pacname = StringUtils.replace(pacname, "/", ".");
      classname = StringUtils.substringAfterLast(sourceRelativePath, "/");
    }
    classname = StringUtils.substringBeforeLast(classname, ".");
    if (StringUtils.isBlank(pacname)) {
      return new StringBuilder().append(JavaPackage.DEFAULT_PACKAGE_NAME).append(".").append(classname).toString();
    } else {
      return new StringBuilder().append(pacname.trim()).append(".").append(classname).toString();
    }
  }

}
