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
package org.sonar.ce.task.projectanalysis.analysis;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.setting.ChildSettings;

@ComputeEngineSide
public class ProjectConfigurationFactory {

  private final Settings globalSettings;
  private final DbClient dbClient;

  public ProjectConfigurationFactory(Settings globalSettings, DbClient dbClient) {
    this.globalSettings = globalSettings;
    this.dbClient = dbClient;
  }

  public Configuration newProjectConfiguration(String projectUuid) {
    Settings projectSettings = new ChildSettings(globalSettings);
    addSettings(projectSettings, projectUuid);
    return new ConfigurationBridge(projectSettings);
  }

  private void addSettings(Settings settings, String componentUuid) {
    try (DbSession session = dbClient.openSession(false)) {
      dbClient.propertiesDao()
        .selectEntityProperties(session, componentUuid)
        .forEach(property -> settings.setProperty(property.getKey(), property.getValue()));
    }
  }
}
