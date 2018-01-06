/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.tests.performance;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

public class MavenLogs {

  /**
   * Total time: 6.015s
   * Total time: 3:14.025s
   */
  public static Long extractTotalTime(String logs) {
    Pattern pattern = Pattern.compile("^.*Total time: (\\d*:)?(\\d+).(\\d+)s.*$", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(logs);
    if (matcher.matches()) {
      String minutes = StringUtils.defaultIfBlank(StringUtils.removeEnd(matcher.group(1), ":"), "0");
      String seconds = StringUtils.defaultIfBlank(matcher.group(2), "0");
      String millis = StringUtils.defaultIfBlank(matcher.group(3), "0");

      return (Long.parseLong(minutes) * 60000) + (Long.parseLong(seconds) * 1000) + Long.parseLong(millis);
    }
    throw new IllegalStateException("Maven logs do not contain \"Total time\"");
  }

  /**
   * Final Memory: 68M/190M
   */
  public static Long extractEndMemory(String logs) {
    return extractLong(logs, ".*Final Memory: (\\d+)M/[\\d]+M.*");
  }

  public static Long extractMaxMemory(String logs) {
    return extractLong(logs, ".*Final Memory: [\\d]+M/(\\d+)M.*");
  }

  private static Long extractLong(String logs, String format) {
    Pattern pattern = Pattern.compile(format);
    Matcher matcher = pattern.matcher(logs);
    if (matcher.matches()) {
      String s = matcher.group(1);
      return Long.parseLong(s);
    }
    return null;
  }
}
