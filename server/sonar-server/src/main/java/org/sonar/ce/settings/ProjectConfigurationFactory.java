/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.ce.settings;

import java.util.Optional;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.db.DbClient;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;

import static org.sonar.db.component.ComponentDto.generateBranchKey;

@ComputeEngineSide
public class ProjectConfigurationFactory {

  private final Settings globalSettings;
  private final DbClient dbClient;

  public ProjectConfigurationFactory(Settings globalSettings, DbClient dbClient) {
    this.globalSettings = globalSettings;
    this.dbClient = dbClient;
  }

  public Configuration newProjectConfiguration(String projectKey, Optional<Branch> branch) {
    Settings projectSettings = new ProjectSettings(globalSettings);
    addSettings(projectSettings, projectKey);
    getBranchName(branch).ifPresent(
      b -> addSettings(projectSettings, generateBranchKey(projectKey, b)));
    return new ConfigurationBridge(projectSettings);
  }

  private void addSettings(Settings settings, String componentDbKey) {
    dbClient.propertiesDao()
      .selectProjectProperties(componentDbKey)
      .forEach(property -> settings.setProperty(property.getKey(), property.getValue()));
  }

  private static Optional<String> getBranchName(Optional<Branch> branchOpt) {
    if (!branchOpt.isPresent()) {
      return Optional.empty();
    }
    Branch branch = branchOpt.get();
    if (!branch.isLegacyFeature() && !branch.isMain()) {
      return branch.getName();
    }
    return Optional.empty();
  }
}
