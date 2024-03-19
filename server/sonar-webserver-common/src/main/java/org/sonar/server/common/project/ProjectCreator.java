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
package org.sonar.server.common.project;

import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbSession;
import org.sonar.db.project.CreationMethod;
import org.sonar.server.common.component.ComponentCreationParameters;
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.common.component.NewComponent;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.user.UserSession;

import static org.sonar.api.resources.Qualifiers.PROJECT;

@ServerSide
public class ProjectCreator {

  private final UserSession userSession;
  private final ProjectDefaultVisibility projectDefaultVisibility;
  private final ComponentUpdater componentUpdater;

  public ProjectCreator(UserSession userSession, ProjectDefaultVisibility projectDefaultVisibility, ComponentUpdater componentUpdater) {
    this.userSession = userSession;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.componentUpdater = componentUpdater;
  }

  public ComponentCreationData createProject(DbSession dbSession, String projectKey, String projectName, @Nullable String mainBranchName, CreationMethod creationMethod,
    @Nullable Boolean isPrivate, boolean isManaged) {
    boolean visibility = isPrivate != null ? isPrivate : projectDefaultVisibility.get(dbSession).isPrivate();
    NewComponent projectComponent = NewComponent.newComponentBuilder()
      .setKey(projectKey)
      .setName(projectName)
      .setPrivate(visibility)
      .setQualifier(PROJECT)
      .build();
    ComponentCreationParameters componentCreationParameters = ComponentCreationParameters.builder()
      .newComponent(projectComponent)
      .userLogin(userSession.getLogin())
      .userUuid(userSession.getUuid())
      .mainBranchName(mainBranchName)
      .isManaged(isManaged)
      .creationMethod(creationMethod)
      .build();
    return componentUpdater.createWithoutCommit(dbSession, componentCreationParameters);
  }

  public ComponentCreationData createProject(DbSession dbSession, String projectKey, String projectName, @Nullable String mainBranchName, CreationMethod creationMethod) {
    return createProject(dbSession, projectKey, projectName, mainBranchName, creationMethod, projectDefaultVisibility.get(dbSession).isPrivate(), false);
  }
}
