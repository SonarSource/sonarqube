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
package org.sonar.server.settings;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.setting.ChildSettings;

import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class ProjectConfigurationLoaderImpl implements ProjectConfigurationLoader {
  private final Settings globalSettings;
  private final DbClient dbClient;

  public ProjectConfigurationLoaderImpl(Settings globalSettings, DbClient dbClient) {
    this.globalSettings = globalSettings;
    this.dbClient = dbClient;
  }

  @Override
  public Map<String, Configuration> loadProjectConfigurations(DbSession dbSession, Set<ComponentDto> projects) {
    Set<String> mainBranchDbKeys = projects.stream().map(ComponentDto::getKey).collect(Collectors.toSet());
    Map<String, ChildSettings> mainBranchSettingsByDbKey = loadMainBranchConfigurations(dbSession, mainBranchDbKeys);
    return projects.stream()
      .collect(uniqueIndex(ComponentDto::uuid, component -> {
        if (component.getDbKey().equals(component.getKey())) {
          return mainBranchSettingsByDbKey.get(component.getKey()).asConfiguration();
        }

        ChildSettings settings = new ChildSettings(mainBranchSettingsByDbKey.get(component.getKey()));
        dbClient.propertiesDao()
            .selectProjectProperties(dbSession, component.getDbKey())
          .forEach(property -> settings.setProperty(property.getKey(), property.getValue()));
        return settings.asConfiguration();
      }));
  }

  private Map<String, ChildSettings> loadMainBranchConfigurations(DbSession dbSession, Set<String> dbKeys) {
    return dbKeys.stream().collect(uniqueIndex(Function.identity(), dbKey -> {
      ChildSettings settings = new ChildSettings(globalSettings);
      List<PropertyDto> propertyDtos = dbClient.propertiesDao()
          .selectProjectProperties(dbSession, dbKey);
      propertyDtos
        .forEach(property -> settings.setProperty(property.getKey(), property.getValue()));
      return settings;
    }));
  }
}
