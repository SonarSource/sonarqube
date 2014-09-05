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
package org.sonar.java.api;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Project;

/**
 * @since 2.6
 * @deprecated in 4.2. See API provided by Java plugins.
 */
@Deprecated
public final class JavaUtils {

  public static final String PACKAGE_SEPARATOR = ".";
  public static final String DEFAULT_PACKAGE = "[default]";

  /**
   * All sensors executed after this barrier are sure that all Java resources are indexed.
   */
  public static final String BARRIER_BEFORE_SQUID = "BEFORE_SQUID";

  /**
   * Sensors executed before this barrier must not rely on index. No Java resources are indexed.
   * Value is 'squid' in order to be backward-compatible with Sensor.FLAG_SQUID_ANALYSIS.
   */
  public static final String BARRIER_AFTER_SQUID = "squid";

  /**
   * To determine value of this property use {@link #getSourceVersion(Project)}.
   */
  public static final String JAVA_SOURCE_PROPERTY = "sonar.java.source";

  /**
   * Default value for property {@link #JAVA_SOURCE_PROPERTY}.
   */
  public static final String JAVA_SOURCE_DEFAULT_VALUE = "1.5";

  /**
   * To determine value of this property use {@link #getTargetVersion(Project)}.
   */
  public static final String JAVA_TARGET_PROPERTY = "sonar.java.target";

  /**
   * Default value for property {@link #JAVA_TARGET_PROPERTY}.
   */
  public static final String JAVA_TARGET_DEFAULT_VALUE = "1.5";

  private JavaUtils() {
    // only static methods
  }

  public static String abbreviatePackage(String packageName) {
    String[] parts = StringUtils.split(packageName, PACKAGE_SEPARATOR);
    StringBuilder sb = new StringBuilder();
    if (parts.length >= 1) {
      sb.append(parts[0]);
    }
    for (int index = 1; index < parts.length; index++) {
      sb.append(PACKAGE_SEPARATOR).append(parts[index].charAt(0));
    }
    return sb.toString();
  }

  public static String getSourceVersion(Project project) {
    String version = project.getSettings() != null ? project.getSettings().getString(JAVA_SOURCE_PROPERTY) : null;
    return StringUtils.isNotBlank(version) ? version : JAVA_SOURCE_DEFAULT_VALUE;
  }

  public static String getTargetVersion(Project project) {
    String version = project.getSettings() != null ? project.getSettings().getString(JAVA_TARGET_PROPERTY) : null;
    return StringUtils.isNotBlank(version) ? version : JAVA_TARGET_DEFAULT_VALUE;
  }
}
