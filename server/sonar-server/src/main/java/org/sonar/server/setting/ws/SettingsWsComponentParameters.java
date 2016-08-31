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
package org.sonar.server.setting.ws;

import org.sonar.api.server.ws.WebService;

import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_COMPONENT_KEY;

public class SettingsWsComponentParameters {

  private SettingsWsComponentParameters() {
    // Only static methods
  }

  static void addComponentParameters(WebService.NewAction action) {
    action.createParam(PARAM_COMPONENT_ID)
      .setDescription("Component id")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_COMPONENT_KEY)
      .setDescription("Component key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

}
