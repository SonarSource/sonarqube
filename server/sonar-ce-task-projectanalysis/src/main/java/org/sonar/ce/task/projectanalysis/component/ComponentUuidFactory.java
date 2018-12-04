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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentWithModuleUuidDto;
import org.sonar.db.component.KeyWithUuidDto;

public class ComponentUuidFactory {
  private final Map<String, String> uuidsByKey = new HashMap<>();

  public ComponentUuidFactory(DbClient dbClient, DbSession dbSession, String rootKey, Map<String, String> reportModulesPath) {
    Map<String, String> modulePathsByUuid;

    if (reportModulesPath.isEmpty()) {
      noMigration(dbClient, dbSession, rootKey);
    } else {
      modulePathsByUuid = loadModulePathsByUuid(dbClient, dbSession, rootKey, reportModulesPath);

      if (modulePathsByUuid.isEmpty()) {
        noMigration(dbClient, dbSession, rootKey);
      } else {
        doMigration(dbClient, dbSession, rootKey, modulePathsByUuid);
      }
    }
  }

  private void noMigration(DbClient dbClient, DbSession dbSession, String rootKey) {
    List<KeyWithUuidDto> keys = dbClient.componentDao().selectUuidsByKeyFromProjectKey(dbSession, rootKey);
    keys.forEach(dto -> uuidsByKey.put(dto.key(), dto.uuid()));
  }

  private void doMigration(DbClient dbClient, DbSession dbSession, String rootKey, Map<String, String> modulePathsByUuid) {
    List<ComponentWithModuleUuidDto> dtos = loadComponentsWithModuleUuid(dbClient, dbSession, rootKey);
    for (ComponentWithModuleUuidDto dto : dtos) {
      if ("/".equals(dto.path())) {
        // skip root folders
        continue;
      }

      if (Scopes.PROJECT.equals(dto.scope())) {
        String modulePathFromRootProject = modulePathsByUuid.get(dto.uuid());
        if (modulePathFromRootProject != null || StringUtils.isEmpty(dto.moduleUuid())) {
          // means that it's a root or a module with a valid path (to avoid overwriting key of root)
          uuidsByKey.put(ComponentKeys.createEffectiveKey(rootKey, modulePathFromRootProject), dto.uuid());
        }
      } else {
        String modulePathFromRootProject = modulePathsByUuid.get(dto.moduleUuid());
        String componentPath = createComponentPath(dto, modulePathFromRootProject);
        uuidsByKey.put(ComponentKeys.createEffectiveKey(rootKey, componentPath), dto.uuid());
      }
    }
  }

  @CheckForNull
  private static String createComponentPath(ComponentWithModuleUuidDto dto, @Nullable String modulePathFromRootProject) {
    if (StringUtils.isEmpty(modulePathFromRootProject)) {
      return dto.path();
    }

    if (StringUtils.isEmpty(dto.path())) {
      // will be the case for modules
      return modulePathFromRootProject;
    }

    return modulePathFromRootProject + "/" + dto.path();
  }

  private static List<ComponentWithModuleUuidDto> loadComponentsWithModuleUuid(DbClient dbClient, DbSession dbSession, String rootKey) {
    return dbClient.componentDao().selectComponentsWithModuleUuidFromProjectKey(dbSession, rootKey);
  }

  private static Map<String, String> loadModulePathsByUuid(DbClient dbClient, DbSession dbSession, String rootKey, Map<String, String> pathByModuleKey) {
    List<ComponentDto> moduleDtos = dbClient.componentDao()
      .selectModulesFromProjectKey(dbSession, rootKey, false).stream()
      .filter(c -> Qualifiers.MODULE.equals(c.qualifier()))
      .collect(Collectors.toList());

    if (moduleDtos.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, String> modulePathByUuid = new HashMap<>();
    for (ComponentDto dto : moduleDtos) {
      String relativePath = pathByModuleKey.get(dto.getKey());
      if (relativePath != null) {
        modulePathByUuid.put(dto.uuid(), relativePath);
      }
    }
    return modulePathByUuid;
  }

  /**
   * Get UUID from database if it exists, otherwise generate a new one.
   */
  public String getOrCreateForKey(String key) {
    return uuidsByKey.computeIfAbsent(key, k -> Uuids.create());
  }
}
