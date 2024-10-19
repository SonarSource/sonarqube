/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.LinkedHashMap;
import java.util.Map;
import org.sonar.application.es.EsInstallation;

class EsServerCliJvmOptions extends JvmOptions<EsServerCliJvmOptions> {

  public EsServerCliJvmOptions(EsInstallation esInstallation) {
    super(mandatoryOptions(esInstallation));
  }

  private static Map<String, String> mandatoryOptions(EsInstallation esInstallation) {
    Map<String, String> res = new LinkedHashMap<>(9);
    res.put("-Xms4m", "");
    res.put("-Xmx64m", "");
    res.put("-XX:+UseSerialGC", "");
    res.put("-Dcli.name=", "server");
    res.put("-Dcli.script=", "./bin/elasticsearch");
    res.put("-Dcli.libs=", "lib/tools/server-cli");
    res.put("-Des.path.home=", esInstallation.getHomeDirectory().getAbsolutePath());
    res.put("-Des.path.conf=", esInstallation.getConfDirectory().getAbsolutePath());
    res.put("-Des.distribution.type=", "tar");
    return res;
  }
}
