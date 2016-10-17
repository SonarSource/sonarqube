/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.index.AuthorizationIndexer;
import org.sonar.server.component.es.ProjectMeasuresIndexer;
import org.sonar.server.test.index.TestIndexer;

@ServerSide
@ComputeEngineSide
public class ComponentCleanerService {

  private final DbClient dbClient;
  private final AuthorizationIndexer issueAuthorizationIndexer;
  private final IssueIndexer issueIndexer;
  private final TestIndexer testIndexer;
  private final ProjectMeasuresIndexer projectMeasuresIndexer;
  private final ResourceTypes resourceTypes;
  private final ComponentFinder componentFinder;

  public ComponentCleanerService(DbClient dbClient, AuthorizationIndexer issueAuthorizationIndexer, IssueIndexer issueIndexer,
                                 TestIndexer testIndexer, ProjectMeasuresIndexer projectMeasuresIndexer, ResourceTypes resourceTypes, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.issueAuthorizationIndexer = issueAuthorizationIndexer;
    this.issueIndexer = issueIndexer;
    this.testIndexer = testIndexer;
    this.projectMeasuresIndexer = projectMeasuresIndexer;
    this.resourceTypes = resourceTypes;
    this.componentFinder = componentFinder;
  }

  public void delete(DbSession dbSession, List<ComponentDto> projects) {
    for (ComponentDto project : projects) {
      delete(dbSession, project);
    }
  }

  public void delete(String projectKey) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      ComponentDto project = componentFinder.getByKey(dbSession, projectKey);
      delete(dbSession, project);
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private void delete(DbSession dbSession, ComponentDto project) {
    if (hasNotProjectScope(project) || isNotDeletable(project)) {
      throw new IllegalArgumentException("Only projects can be deleted");
    }
    dbClient.purgeDao().deleteProject(dbSession, project.uuid());
    dbSession.commit();

    deleteFromIndices(project.uuid());
  }

  private void deleteFromIndices(String projectUuid) {
    // optimization : index "issues" is refreshed once at the end
    issueAuthorizationIndexer.deleteProject(projectUuid, false);
    issueIndexer.deleteProject(projectUuid);
    testIndexer.deleteByProject(projectUuid);
    projectMeasuresIndexer.deleteProject(projectUuid);
  }

  private static boolean hasNotProjectScope(ComponentDto project) {
    return !Scopes.PROJECT.equals(project.scope());
  }

  private boolean isNotDeletable(ComponentDto project) {
    ResourceType resourceType = resourceTypes.get(project.qualifier());
    return resourceType == null || !resourceType.getBooleanProperty("deletable");
  }
}
