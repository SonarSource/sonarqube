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
package org.sonar.application.command;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class WebJvmOptions extends JvmOptions<WebJvmOptions> {
  public WebJvmOptions(File tmpDir, JavaVersion javaVersion) {
    super(mandatoryOptions(tmpDir, javaVersion));
  }

  private static Map<String, String> mandatoryOptions(File tmpDir, JavaVersion javaVersion) {
    Map<String, String> res = new LinkedHashMap<>(3);
    res.put("-Djava.awt.headless=", "true");
    res.put("-Dfile.encoding=", "UTF-8");
    res.put("-Djava.io.tmpdir=", tmpDir.getAbsolutePath());

    if (javaVersion.isAtLeastJava11()) {
      // avoid illegal reflective access operations done by MyBatis
      res.put("--add-opens=java.base/java.util=ALL-UNNAMED", "");

      // avoid illegal reflective access operations done by Tomcat
      res.put("--add-opens=java.base/java.lang=ALL-UNNAMED", "");
      res.put("--add-opens=java.base/java.io=ALL-UNNAMED", "");
      res.put("--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED", "");
    }
    return res;
  }
}
