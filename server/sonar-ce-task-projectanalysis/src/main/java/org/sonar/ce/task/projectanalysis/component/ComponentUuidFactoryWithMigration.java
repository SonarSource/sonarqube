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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentWithModuleUuidDto;
import org.sonar.db.component.KeyWithUuidDto;

public class ComponentUuidFactoryWithMigration implements ComponentUuidFactory {
  private final Map<String, String> uuidsByDbKey = new HashMap<>();
  private final Map<String, String> uuidsByMigratedKey = new HashMap<>();

  public ComponentUuidFactoryWithMigration(DbClient dbClient, DbSession dbSession, String rootKey, Branch branch,
    Function<String, String> pathToKey, Map<String, String> reportModulesPath) {
    Map<String, String> modulePathsByUuid;
    List<KeyWithUuidDto> keys;
    if (branch.isMain()) {
      keys = dbClient.componentDao().selectUuidsByKeyFromProjectKey(dbSession, rootKey);
    } else if (branch.getType() == BranchType.PULL_REQUEST) {
      keys = dbClient.componentDao().selectUuidsByKeyFromProjectKeyAndPullRequest(dbSession, rootKey, branch.getPullRequestKey());
    } else {
      keys = dbClient.componentDao().selectUuidsByKeyFromProjectKeyAndBranch(dbSession, rootKey, branch.getName());
    }

    keys.forEach(dto -> uuidsByDbKey.put(dto.key(), dto.uuid()));

    if (!reportModulesPath.isEmpty()) {
      modulePathsByUuid = loadModulePathsByUuid(dbClient, dbSession, rootKey, branch, reportModulesPath);

      if (!modulePathsByUuid.isEmpty()) {
        doMigration(dbClient, dbSession, rootKey, pathToKey, modulePathsByUuid);
      }
    }
  }

  private void doMigration(DbClient dbClient, DbSession dbSession, String rootKey, Function<String, String> pathToKey, Map<String, String> modulePathsByUuid) {
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
          pathToKey.apply(modulePathFromRootProject);
          uuidsByMigratedKey.put(pathToKey.apply(modulePathFromRootProject), dto.uuid());
        }
      } else {
        String modulePathFromRootProject = modulePathsByUuid.get(dto.moduleUuid());
        String componentPath = createComponentPath(dto, modulePathFromRootProject);
        uuidsByMigratedKey.put(pathToKey.apply(componentPath), dto.uuid());
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
    return dbClient.componentDao().selectEnabledComponentsWithModuleUuidFromProjectKey(dbSession, rootKey);
  }

  private static Map<String, String> loadModulePathsByUuid(DbClient dbClient, DbSession dbSession, String rootKey,
    Branch branch, Map<String, String> pathByModuleKey) {
    String branchKey = branch.getType() == BranchType.BRANCH ? branch.getName() : null;
    String prKey = branch.getType() == BranchType.PULL_REQUEST ? branch.getPullRequestKey() : null;

    List<ComponentDto> moduleDtos = dbClient.componentDao()
      .selectProjectAndModulesFromProjectKey(dbSession, rootKey, true, branchKey, prKey).stream()
      .filter(c -> Qualifiers.MODULE.equals(c.qualifier()))
      .toList();

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
   * Get UUID from component having the same key in database if it exists, otherwise look for migrated keys, and finally generate a new one.
   */
  @Override
  public String getOrCreateForKey(String key) {
    return uuidsByDbKey.computeIfAbsent(key, k1 -> uuidsByMigratedKey.computeIfAbsent(k1, k2 -> Uuids.create()));
  }
}
