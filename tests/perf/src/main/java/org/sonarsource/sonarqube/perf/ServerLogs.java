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
package org.sonarsource.sonarqube.perf;

import com.sonar.orchestrator.Orchestrator;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
    if (orch.getServer() != null && orch.getServer().getLogs() != null) {
      FileUtils.write(orch.getServer().getLogs(), "", false);
    }
  }

}
