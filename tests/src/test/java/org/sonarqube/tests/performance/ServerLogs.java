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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.container.Server;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

public class ServerLogs {

  public static Date extractDate(String line) {
    String pattern = "yyyy.MM.dd HH:mm:ss";
    SimpleDateFormat format = new SimpleDateFormat(pattern);
    if (line.length() > 19) {
      try {
        return format.parse(line.substring(0, 19));
      } catch (Exception e) {
        // ignore
      }
    }
    return null;
  }

  public static Date extractFirstDate(List<String> lines) {
    for (String line : lines) {
      Date d = ServerLogs.extractDate(line);
      if (d != null) {
        return d;
      }
    }
    return null;
  }

  public static void clear(Orchestrator orch) throws IOException {
    Server server = orch.getServer();
    if (server != null) {
      for (File file : new File[]{server.getAppLogs(), server.getWebLogs(), server.getCeLogs(), server.getEsLogs()}) {
        if (file != null) {
          FileUtils.write(file, "", false);
        }
      }
    }
  }

  /**
   * 2015.09.29 16:57:45 INFO ce[o.s.s.c.q.CeWorkerRunnableImpl] Executed task | project=com.github.kevinsawicki:http-request-parent | id=AVAZm9oHIXrp54OmOeQe | time=2283ms
   */
  public static Long extractComputationTotalTime(Orchestrator orchestrator) throws IOException {
    File report = orchestrator.getServer().getCeLogs();
    List<String> logsLines = FileUtils.readLines(report, Charsets.UTF_8);
    return extractComputationTotalTime(logsLines);
  }

  static Long extractComputationTotalTime(List<String> logs) {
    Pattern pattern = Pattern.compile(".*INFO.*Executed task.* \\| time=(\\d+)ms.*");
    for (int i = logs.size() - 1; i >= 0; i--) {
      String line = logs.get(i);
      Matcher matcher = pattern.matcher(line);
      if (matcher.matches()) {
        String duration = matcher.group(1);
        return Long.parseLong(duration);
      }
    }

    return null;
  }

}
