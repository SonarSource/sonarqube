/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.core.util;

import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.StringUtils.trim;

public final class SettingFormatter {
  private SettingFormatter() {
    // util class
  }

  public static String fromJavaPropertyToEnvVariable(String property) {
    return property.toUpperCase(Locale.ENGLISH).replace('.', '_').replace('-', '_');
  }

  /**
   * Value is split and trimmed.
   */
  public static String[] getStringArrayBySeparator(String value, String separator) {
    String[] strings = StringUtils.splitByWholeSeparator(value, separator);
    String[] result = new String[strings.length];
    for (int index = 0; index < strings.length; index++) {
      result[index] = trim(strings[index]);
    }
    return result;
  }
}
