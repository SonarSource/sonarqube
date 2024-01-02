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
package org.sonar.server.setting;

import java.util.List;
import javax.annotation.Nullable;
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
  public Configuration loadBranchConfiguration(DbSession dbSession, BranchDto branch) {
    return loadProjectAndBranchConfiguration(dbSession, branch.getProjectUuid(), branch.getUuid());
  }

  @Override
  public Configuration loadProjectConfiguration(DbSession dbSession, String projectUuid) {
    return loadProjectAndBranchConfiguration(dbSession, projectUuid, null);
  }

  private Configuration loadProjectAndBranchConfiguration(DbSession dbSession, String projectUuid, @Nullable String branchUuid) {
    ChildSettings projectSettings = internalLoadProjectConfiguration(dbSession, projectUuid);

    if (branchUuid == null) {
      return projectSettings.asConfiguration();
    }

    ChildSettings settings = new ChildSettings(projectSettings);
    dbClient.propertiesDao()
      .selectEntityProperties(dbSession, branchUuid)
      .forEach(property -> settings.setProperty(property.getKey(), property.getValue()));
    return settings.asConfiguration();
  }

  private ChildSettings internalLoadProjectConfiguration(DbSession dbSession, String uuid) {
    ChildSettings settings = new ChildSettings(globalSettings);
    List<PropertyDto> propertyDtos = dbClient.propertiesDao().selectEntityProperties(dbSession, uuid);
    propertyDtos.forEach(property -> settings.setProperty(property.getKey(), property.getValue()));
    return settings;
  }

}
