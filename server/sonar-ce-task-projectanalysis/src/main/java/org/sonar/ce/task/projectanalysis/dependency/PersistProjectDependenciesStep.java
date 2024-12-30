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
package org.sonar.ce.task.projectanalysis.dependency;

import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.dependency.ProjectDependencyDto;

public class PersistProjectDependenciesStep implements ComputationStep {

  private final DbClient dbClient;
  private final ProjectDependenciesHolder projectDependenciesHolder;
  private final TreeRootHolder treeRootHolder;
  private final Clock clock;

  public PersistProjectDependenciesStep(DbClient dbClient, ProjectDependenciesHolder projectDependenciesHolder, TreeRootHolder treeRootHolder, Clock clock) {
    this.dbClient = dbClient;
    this.projectDependenciesHolder = projectDependenciesHolder;
    this.treeRootHolder = treeRootHolder;
    this.clock = clock;
  }

  @Override
  public void execute(Context context) {
    try (DbSession dbSession = dbClient.openSession(true)) {
      Map<String, ProjectDependencyDto> existingDtosByUuids = indexExistingDtosByUuids(dbSession);
      projectDependenciesHolder.getDependencies().forEach(dependency -> updateOrInsertDependency(dbSession, dependency, existingDtosByUuids));
      deleteRemainingDependencies(dbSession, existingDtosByUuids.keySet());
      dbSession.commit();
    }
  }

  private void deleteRemainingDependencies(DbSession dbSession, Set<String> uuidsToBeDeleted) {
    uuidsToBeDeleted.forEach(uuid -> dbClient.projectDependenciesDao().deleteByUuid(dbSession, uuid));
  }

  private Map<String, ProjectDependencyDto> indexExistingDtosByUuids(DbSession dbSession) {
    return dbClient.projectDependenciesDao().selectByBranchUuid(dbSession, treeRootHolder.getRoot().getUuid()).stream()
      .collect(Collectors.toMap(ProjectDependencyDto::uuid, Function.identity()));
  }

  private void updateOrInsertDependency(DbSession dbSession, ProjectDependency dependency, Map<String, ProjectDependencyDto> existingDtosByUuids) {
    ProjectDependencyDto existingDependency = existingDtosByUuids.remove(dependency.getUuid());
    long now = clock.millis();
    if (existingDependency == null) {
      dbClient.projectDependenciesDao().insert(dbSession, toDto(dependency, now, now));
    } else if (shouldUpdate(existingDependency, dependency)) {
      dbClient.projectDependenciesDao().update(dbSession, toDto(dependency, existingDependency.createdAt(), now));
    }
  }

  private static ProjectDependencyDto toDto(ProjectDependency dependency, long createdAt, long updatedAt) {
    return new ProjectDependencyDto(dependency.getUuid(), dependency.getVersion(), null, dependency.getPackageManager(), createdAt, updatedAt);
  }

  private static boolean shouldUpdate(ProjectDependencyDto existing, ProjectDependency target) {
    return !StringUtils.equals(existing.version(), target.getVersion()) ||
      !StringUtils.equals(existing.packageManager(), target.getPackageManager());
  }

  @Override
  public String getDescription() {
    return "Persist project dependencies";
  }
}
