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
package org.sonar.java.api;

import org.apache.commons.lang.StringUtils;

public final class JavaUtils {

  public static final String PACKAGE_SEPARATOR = ".";

  /**
   * All sensors executed after this barrier are sure that all Java resources are indexed.
   */
  public static final String BARRIER_BEFORE_SQUID = "BEFORE_SQUID";

  /**
   * Sensors executed before this barrier must not rely on index. No Java resources are indexed.
   * Value is 'squid' in order to be backward-compatible with Sensor.FLAG_SQUID_ANALYSIS.
   */
  public static final String BARRIER_AFTER_SQUID = "squid";

  private JavaUtils() {
    // only static methods
  }

  public static String abbreviatePackage(String packageName) {
    String[] parts = StringUtils.split(packageName, PACKAGE_SEPARATOR);
    StringBuilder sb = new StringBuilder();
    if (parts.length>=1) {
      sb.append(parts[0]);
    }
    for (int index=1 ; index<parts.length ; index++) {
      sb.append(PACKAGE_SEPARATOR).append(parts[index].charAt(0));
    }
    return sb.toString();
  }
}