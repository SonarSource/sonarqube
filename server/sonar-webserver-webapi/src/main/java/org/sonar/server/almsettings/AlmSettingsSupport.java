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

package org.sonar.server.almsettings;

import org.sonar.db.alm.setting.ALM;
import org.sonarqube.ws.AlmSettings;

import static java.lang.String.format;

class AlmSettingsSupport {

  private AlmSettingsSupport() {
    // Only static methods here for the moment
  }

  static AlmSettings.Alm toAlmWs(ALM alm) {
    switch (alm) {
      case GITHUB:
        return AlmSettings.Alm.github;
      case BITBUCKET:
        return AlmSettings.Alm.bitbucket;
      case AZURE_DEVOPS:
        return AlmSettings.Alm.azure;
      default:
        throw new IllegalStateException(format("Unknown ALM '%s'", alm.name()));
    }
  }
}
