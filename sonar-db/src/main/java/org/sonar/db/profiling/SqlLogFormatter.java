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
package org.sonar.db.profiling;

import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.apache.commons.lang.StringUtils.replace;

public enum SqlLogFormatter {
  FORMATTER;

  public static final String PARAM_NULL = "[null]";
  public static final int PARAM_MAX_WIDTH = 500;

  public String formatSql(String sql) {
    return StringUtils.replaceChars(sql, '\n', ' ');
  }

  public String formatParam(@Nullable Object param) {
    if (param == null) {
      return PARAM_NULL;
    }
    return replace(abbreviate(param.toString(), PARAM_MAX_WIDTH), "\n", "\\n");
  }

  public String formatParams(Object[] params) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < params.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(formatParam(params[i]));
    }
    return sb.toString();
  }
}
