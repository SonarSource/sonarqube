/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.profiling;

import java.util.regex.Pattern;
import javax.annotation.Nullable;

import static org.apache.commons.lang.StringUtils.abbreviate;

public class SqlLogFormatter {

  public static final int PARAM_MAX_WIDTH = 500;
  private static final String PARAM_NULL = "[null]";
  private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\n");

  private SqlLogFormatter() {
    // only statics
  }

  public static String reformatSql(String sql) {
    char[] chars = sql.toCharArray();
    StringBuilder result = new StringBuilder(chars.length);

    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];
      if (c == '\n' || c == '\t') {
        c = ' ';
      }
      if (Character.isWhitespace(c) && i > 0 && Character.isWhitespace(chars[i - 1])) {
        continue;
      }
      result.append(c);
    }
    return result.toString();
  }

  public static String reformatParam(@Nullable Object param) {
    if (param == null) {
      return PARAM_NULL;
    }
    String abbreviated = abbreviate(param.toString(), PARAM_MAX_WIDTH);
    return NEWLINE_PATTERN.matcher(abbreviated).replaceAll("\\\\n");
  }

  public static String reformatParams(Object[] params) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < params.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(reformatParam(params[i]));
    }
    return sb.toString();
  }

  public static int countArguments(String sql) {
    int argCount = 0;
    for (int i = 0; i < sql.length(); i++) {
      if (sql.charAt(i) == '?') {
        argCount++;
      }
    }
    return argCount;
  }
}
