/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.pushevent.PushEventDto;
import org.sonar.server.es.Indexers;
import org.sonar.server.project.Project;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.project.RekeyedProject;
import org.sonar.server.user.UserSession;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.sonar.core.component.ComponentKeys.checkProjectKey;

@ServerSide
public class ComponentService {
  private static final Gson GSON = new GsonBuilder().create();

  private final DbClient dbClient;
  private final UserSession userSession;
  private final Indexers indexers;
  private final ProjectLifeCycleListeners projectLifeCycleListeners;

  public ComponentService(DbClient dbClient, UserSession userSession, Indexers indexers, ProjectLifeCycleListeners projectLifeCycleListeners) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.indexers = indexers;
    this.projectLifeCycleListeners = projectLifeCycleListeners;
  }

  public void updateKey(DbSession dbSession, ProjectDto project, String newKey) {
    userSession.checkEntityPermission(UserRole.ADMIN, project);
    checkProjectKey(newKey);
    dbClient.componentKeyUpdaterDao().updateKey(dbSession, project.getUuid(), project.getKey(), newKey);
    indexers.commitAndIndexEntities(dbSession, singletonList(project), Indexers.EntityEvent.PROJECT_KEY_UPDATE);
    Project newProject = new Project(project.getUuid(), newKey, project.getName(), project.getDescription(), project.getTags());
    projectLifeCycleListeners.onProjectsRekeyed(singleton(new RekeyedProject(newProject, project.getKey())));
    persistEvent(project, newKey);
  }

  private void persistEvent(ProjectDto project, String newProjectKey) {
    ProjectKeyChangedEvent event = new ProjectKeyChangedEvent(project.getKey(), newProjectKey);
    try (DbSession dbSession = dbClient.openSession(false)) {
      PushEventDto eventDto = new PushEventDto()
        .setName("ProjectKeyChanged")
        .setProjectUuid(project.getUuid())
        .setPayload(serializeIssueToPushEvent(event));
      dbClient.pushEventDao().insert(dbSession, eventDto);
      dbSession.commit();
    }
  }

  private static byte[] serializeIssueToPushEvent(ProjectKeyChangedEvent event) {
    return GSON.toJson(event).getBytes(UTF_8);
  }

}
