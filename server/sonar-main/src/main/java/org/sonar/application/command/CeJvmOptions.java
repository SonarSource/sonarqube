/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

public class CeJvmOptions extends JvmOptions<CeJvmOptions> {

  public CeJvmOptions(File tmpDir) {
    super(mandatoryOptions(tmpDir));
  }

  private static Map<String, String> mandatoryOptions(File tmpDir) {
    Map<String, String> res = LinkedHashMap.newLinkedHashMap(3);
    res.put("-Djava.awt.headless=", "true");
    res.put("-Dfile.encoding=", "UTF-8");
    res.put("-Djava.io.tmpdir=", tmpDir.getAbsolutePath());
    res.put("-XX:-OmitStackTraceInFastThrow", "");
    // avoid illegal reflective access operations done by MyBatis
    res.put("--add-opens=java.base/java.util=ALL-UNNAMED", "");

    // avoid illegal reflective access operations done by Hazelcast
    res.put("--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED", "");
    res.put("--add-opens=java.base/java.lang=ALL-UNNAMED", "");
    res.put("--add-opens=java.base/java.nio=ALL-UNNAMED", "");
    res.put("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED", "");
    res.put("--add-opens=java.management/sun.management=ALL-UNNAMED", "");
    res.put("--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED", "");

    return res;
  }
}
