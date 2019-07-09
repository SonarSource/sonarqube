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
package org.sonar.ce.task.projectanalysis.analysis;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.db.DbClient;
import org.sonar.db.component.BranchType;
import org.sonar.server.setting.ChildSettings;

import static org.sonar.db.component.ComponentDto.generateBranchKey;
import static org.sonar.db.component.ComponentDto.generatePullRequestKey;

@ComputeEngineSide
public class ProjectConfigurationFactory {

  private final Settings globalSettings;
  private final DbClient dbClient;

  public ProjectConfigurationFactory(Settings globalSettings, DbClient dbClient) {
    this.globalSettings = globalSettings;
    this.dbClient = dbClient;
  }

  public Configuration newProjectConfiguration(String projectKey, Branch branch) {
    Settings projectSettings = new ChildSettings(globalSettings);
    addSettings(projectSettings, projectKey);
    if (branch.getType() == BranchType.PULL_REQUEST) {
      addSettings(projectSettings, generatePullRequestKey(projectKey, branch.getPullRequestKey()));
    } else {
      addSettings(projectSettings, generateBranchKey(projectKey, branch.getName()));
    }
    return new ConfigurationBridge(projectSettings);
  }

  private void addSettings(Settings settings, String componentDbKey) {
    dbClient.propertiesDao()
      .selectProjectProperties(componentDbKey)
      .forEach(property -> settings.setProperty(property.getKey(), property.getValue()));
  }
}
