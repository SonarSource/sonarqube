/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.setting;

import java.util.List;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.property.PropertyDto;

public class ProjectConfigurationLoaderImpl implements ProjectConfigurationLoader {
  private final Settings globalSettings;
  private final DbClient dbClient;

  public ProjectConfigurationLoaderImpl(Settings globalSettings, DbClient dbClient) {
    this.globalSettings = globalSettings;
    this.dbClient = dbClient;
  }

  @Override
  public Configuration loadProjectConfiguration(DbSession dbSession, BranchDto branchDto) {
    if (branchDto.isMain()) {
      ChildSettings mainBranchSettings = loadMainBranchConfiguration(dbSession, branchDto.getUuid());
      return mainBranchSettings.asConfiguration();
    } else {
      BranchDto mainBranch = dbClient.branchDao().selectMainBranchByProjectUuid(dbSession, branchDto.getProjectUuid())
        .orElseThrow(() -> new IllegalStateException("Main branch not found for project: " + branchDto.getProjectUuid()));

      ChildSettings mainBranchSettings = loadMainBranchConfiguration(dbSession, mainBranch.getUuid());
      ChildSettings settings = new ChildSettings(mainBranchSettings);
      dbClient.propertiesDao()
        .selectComponentProperties(dbSession, branchDto.getUuid())
        .forEach(property -> settings.setProperty(property.getKey(), property.getValue()));
      return settings.asConfiguration();
    }
  }

  private ChildSettings loadMainBranchConfiguration(DbSession dbSession, String uuid) {
    ChildSettings settings = new ChildSettings(globalSettings);
    List<PropertyDto> propertyDtos = dbClient.propertiesDao().selectComponentProperties(dbSession, uuid);
    propertyDtos.forEach(property -> settings.setProperty(property.getKey(), property.getValue()));
    return settings;
  }
}
