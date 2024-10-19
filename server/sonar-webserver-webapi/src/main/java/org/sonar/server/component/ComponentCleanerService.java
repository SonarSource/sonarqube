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
package org.sonar.server.component;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.Indexers;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;

@ServerSide
public class ComponentCleanerService {

  private static final Logger logger = LoggerFactory.getLogger(ComponentCleanerService.class);

  private final DbClient dbClient;
  private final Indexers indexers;

  public ComponentCleanerService(DbClient dbClient, Indexers indexers) {
    this.dbClient = dbClient;
    this.indexers = indexers;
  }

  public void delete(DbSession dbSession, List<ProjectDto> projects, String user) {
    for (ProjectDto project : projects) {
      deleteEntity(dbSession, project, user);
    }
  }

  public void deleteBranch(DbSession dbSession, BranchDto branch) {
    if (branch.isMain()) {
      throw new IllegalArgumentException("Only non-main branches can be deleted");
    }
    dbClient.purgeDao().deleteBranch(dbSession, branch.getUuid());
    updateProjectNcloc(dbSession, branch.getProjectUuid());
    indexers.commitAndIndexBranches(dbSession, singletonList(branch), Indexers.BranchEvent.DELETION);
  }

  private void updateProjectNcloc(DbSession dbSession, String projectUuid) {
    long maxncloc = dbClient.liveMeasureDao().findNclocOfBiggestBranchForProject(dbSession, projectUuid);
    dbClient.projectDao().updateNcloc(dbSession, projectUuid, maxncloc);
  }

  public void deleteEntity(DbSession dbSession, EntityDto entity, String user) {
    OrganizationDto organization = dbClient.organizationDao().selectByUuid(dbSession, entity.getOrganizationUuid())
        .orElseThrow(() -> new IllegalStateException("No organization found: " + entity.getOrganizationUuid()));
    logger.info("Cleaning component entries for projectName: {}, projectKey: {}, projectId: {}, organization: {}, orgId: {}, user: {}",
        entity.getName(), entity.getKey(), entity.getUuid(), organization.getKey(),
        organization.getUuid(), user);

    checkArgument(!entity.getQualifier().equals(Qualifiers.SUBVIEW), "Qualifier can't be subview");
    dbClient.purgeDao().deleteProject(dbSession, entity.getUuid(), entity.getQualifier(), entity.getName(), entity.getKey());
    dbClient.userDao().cleanHomepage(dbSession, entity);
    if (Qualifiers.PROJECT.equals(entity.getQualifier())) {
      dbClient.userTokenDao().deleteByProjectUuid(dbSession, entity.getKey(), entity.getUuid());
    }
    // Note that we do not send an event for each individual branch being deleted with the project
    indexers.commitAndIndexEntities(dbSession, singletonList(entity), Indexers.EntityEvent.DELETION);
  }
}
