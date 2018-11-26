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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentWithModuleUuidDto;
import org.sonar.db.component.KeyWithUuidDto;

public class ComponentUuidFactory {
  private final Map<String, String> uuidsByKey = new HashMap<>();

  public ComponentUuidFactory(DbClient dbClient, DbSession dbSession, String rootKey, Supplier<Map<String, String>> reportModulesPath) {
    Map<String, String> modulePathsByUuid = loadModulePathsByUuid(dbClient, dbSession, rootKey, reportModulesPath);

    if (modulePathsByUuid.isEmpty()) {
      // only contains root project or we don't have relative paths for other modules anyway
      List<KeyWithUuidDto> keys = dbClient.componentDao().selectUuidsByKeyFromProjectKey(dbSession, rootKey);
      keys.forEach(dto -> uuidsByKey.put(dto.key(), dto.uuid()));
    } else {
      List<ComponentWithModuleUuidDto> dtos = loadComponentsWithModuleUuid(dbClient, dbSession, rootKey);
      for (ComponentWithModuleUuidDto dto : dtos) {
        String pathFromRootProject = modulePathsByUuid.get(dto.moduleUuid());
        String componentPath = StringUtils.isEmpty(pathFromRootProject) ? dto.path() : (pathFromRootProject + "/" + dto.path());
        uuidsByKey.put(ComponentKeys.createEffectiveKey(rootKey, componentPath), dto.uuid());
      }
    }
  }

  private static List<ComponentWithModuleUuidDto> loadComponentsWithModuleUuid(DbClient dbClient, DbSession dbSession, String rootKey) {
    return dbClient.componentDao().selectComponentsWithModuleUuidFromProjectKey(dbSession, rootKey);
  }

  private static Map<String, String> loadModulePathsByUuid(DbClient dbClient, DbSession dbSession, String rootKey, Supplier<Map<String, String>> reportModulesPath) {
    List<ComponentDto> moduleDtos = dbClient.componentDao()
      .selectEnabledModulesFromProjectKey(dbSession, rootKey, false).stream()
      .filter(c -> Qualifiers.MODULE.equals(c.qualifier()))
      .collect(Collectors.toList());

    if (moduleDtos.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, String> pathByModuleKey = reportModulesPath.get();
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
