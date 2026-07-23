/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.component;

import java.util.List;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.Indexers.BranchEvent;
import org.sonar.server.es.Indexers.EntityEvent;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.server.db.repository.IssueCountHistoryRepository;
import org.sonarsource.history.server.db.repository.MeasureHistoryRepository;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;

@ServerSide
public class ComponentCleanerService {

  private final DbClient dbClient;
  private final Indexers indexers;
  private final IssueCountHistoryRepository issueCountHistoryRepository;
  private final MeasureHistoryRepository measureHistoryRepository;

  public ComponentCleanerService(
    DbClient dbClient,
    Indexers indexers,
    IssueCountHistoryRepository issueCountHistoryRepository,
    MeasureHistoryRepository measureHistoryRepository) {
    this.dbClient = dbClient;
    this.indexers = indexers;
    this.issueCountHistoryRepository = issueCountHistoryRepository;
    this.measureHistoryRepository = measureHistoryRepository;
  }

  public void delete(DbSession dbSession, List<ProjectDto> projects) {
    for (ProjectDto project : projects) {
      deleteEntity(dbSession, project);
    }
  }

  public void deleteBranch(DbSession dbSession, BranchDto branch) {
    if (branch.isMain()) {
      throw new IllegalArgumentException("Only non-main branches can be deleted");
    }
    deleteHistoryForEntity(dbSession, branch.getUuid(), EntityType.PROJECT_BRANCH);
    dbClient.purgeDao().deleteBranch(dbSession, branch.getUuid());
    updateProjectNcloc(dbSession, branch.getProjectUuid());
    indexers.commitAndIndexBranches(dbSession, singletonList(branch), BranchEvent.DELETION);
  }

  private void updateProjectNcloc(DbSession dbSession, String projectUuid) {
    List<String> branchUuids = dbClient.branchDao().selectByProjectUuid(dbSession, projectUuid).stream()
      .map(BranchDto::getUuid)
      .toList();
    long maxncloc = dbClient.measureDao().findNclocOfBiggestBranch(dbSession, branchUuids);
    dbClient.projectDao().updateNcloc(dbSession, projectUuid, maxncloc);
  }

  public void deleteEntity(DbSession dbSession, EntityDto entity) {
    checkArgument(!entity.getQualifier().equals(ComponentQualifiers.SUBVIEW), "Qualifier can't be subview");
    EntityType entityType = getEntityTypeForQualifier(entity.getQualifier());
    if (entity.isProjectOrApp()) {
      // delete history for all project and application branches
      dbClient.branchDao().selectByProjectUuid(dbSession, entity.getUuid())
        .forEach(branchDto -> deleteHistoryForEntity(dbSession, branchDto.getUuid(), entityType));
    } else {
      deleteHistoryForEntity(dbSession, entity.getUuid(), entityType);
    }
    dbClient.purgeDao().deleteProject(dbSession, entity.getUuid(), entity.getQualifier(), entity.getName(), entity.getKey());
    dbClient.userDao().cleanHomepage(dbSession, entity);
    if (ComponentQualifiers.PROJECT.equals(entity.getQualifier())) {
      dbClient.userTokenDao().deleteByProjectUuid(dbSession, entity.getKey(), entity.getUuid());
    }
    // Note that we do not send an event for each individual branch being deleted with the project
    indexers.commitAndIndexEntities(dbSession, singletonList(entity), EntityEvent.DELETION);
  }

  private void deleteHistoryForEntity(DbSession dbSession, String entityId, EntityType entityType) {
    issueCountHistoryRepository.deleteHistoryForEntity(dbSession, entityId, entityType);
    measureHistoryRepository.deleteHistoryForEntity(dbSession, entityId, entityType);
  }

  private static EntityType getEntityTypeForQualifier(String qualifier) {
    return switch(qualifier) {
      case ComponentQualifiers.PROJECT -> EntityType.PROJECT_BRANCH;
      case ComponentQualifiers.VIEW -> EntityType.PORTFOLIO;
      case ComponentQualifiers.APP -> EntityType.APPLICATION;
      default -> throw new IllegalArgumentException("Unsupported component qualifier '%s'".formatted(qualifier));
    };
  }
}
