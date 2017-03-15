/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;

import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ADD_PROJECT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_UUID;

public class AddProjectAction implements QProfileWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final Languages languages;
  private final ComponentFinder componentFinder;
  private final QProfileWsSupport wsSupport;

  public AddProjectAction(DbClient dbClient, UserSession userSession, Languages languages, ComponentFinder componentFinder, QProfileWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.languages = languages;
    this.componentFinder = componentFinder;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction(ACTION_ADD_PROJECT)
      .setSince("5.2")
      .setDescription("Associate a project with a quality profile.")
      .setPost(true)
      .setHandler(this);

    QProfileReference.defineParams(action, languages);
    QProfileWsSupport.createOrganizationParam(action).setSince("6.4");

    action.createParam(PARAM_PROJECT_UUID)
      .setDescription("A project UUID. Either this parameter, or projectKey must be set.")
      .setExampleValue("69e57151-be0d-4157-adff-c06741d88879");
    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("A project key. Either this parameter, or projectUuid must be set.")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    // fail fast if not logged in
    userSession.checkLoggedIn();

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = loadProject(dbSession, request);
      QualityProfileDto profile = wsSupport.getProfile(dbSession, QProfileReference.from(request));

      if (!profile.getOrganizationUuid().equals(project.getOrganizationUuid())) {
        throw new IllegalArgumentException("Project and Quality profile must have same organization");
      }

      QualityProfileDto currentProfile = dbClient.qualityProfileDao().selectByProjectAndLanguage(dbSession, project.key(), profile.getLanguage());
      if (currentProfile == null) {
        // project uses the default profile
        dbClient.qualityProfileDao().insertProjectProfileAssociation(project.uuid(), profile.getKey(), dbSession);
        dbSession.commit();
      } else if (!profile.getKey().equals(currentProfile.getKey())) {
        dbClient.qualityProfileDao().updateProjectProfileAssociation(project.uuid(), profile.getKey(), currentProfile.getKey(), dbSession);
        dbSession.commit();
      }
    }

    response.noContent();
  }

  private ComponentDto loadProject(DbSession dbSession, Request request) {
    String projectKey = request.param(PARAM_PROJECT_KEY);
    String projectUuid = request.param(PARAM_PROJECT_UUID);
    ComponentDto project = componentFinder.getByUuidOrKey(dbSession, projectUuid, projectKey, ComponentFinder.ParamNames.PROJECT_UUID_AND_KEY);
    checkAdministrator(project);
    return project;
  }

  private void checkAdministrator(ComponentDto project) {
    if (!userSession.hasPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, project.getOrganizationUuid()) &&
      !userSession.hasComponentPermission(UserRole.ADMIN, project)) {
      throw new ForbiddenException("Insufficient privileges");
    }
  }
}
