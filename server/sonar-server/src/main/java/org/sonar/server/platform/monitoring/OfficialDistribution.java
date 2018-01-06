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
package org.sonar.server.platform.monitoring;

import java.io.File;
import org.sonar.api.server.ServerSide;
import org.sonar.server.platform.ServerFileSystem;

@ServerSide
public class OfficialDistribution {
  static final String BRANDING_FILE_PATH = "web/WEB-INF/classes/com/sonarsource/branding";

  private final ServerFileSystem serverFileSystem;

  public OfficialDistribution(ServerFileSystem serverFileSystem) {
    this.serverFileSystem = serverFileSystem;
  }

  public boolean check() {
    // the dependency com.sonarsource:sonarsource-branding is shaded to webapp
    // during release (see sonar-web pom)
    File brandingFile = new File(serverFileSystem.getHomeDir(), BRANDING_FILE_PATH);
    // no need to check that the file exists. java.io.File#length() returns zero in this case.
    return brandingFile.length() > 0L;
  }
}
