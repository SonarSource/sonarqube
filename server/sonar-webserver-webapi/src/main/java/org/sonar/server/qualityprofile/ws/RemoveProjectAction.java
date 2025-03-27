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
package org.sonar.server.qualityprofile.ws;

import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.user.UserSession;

import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_REMOVE_PROJECT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT;

public class RemoveProjectAction implements QProfileWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final Languages languages;
  private final ComponentFinder componentFinder;
  private final QProfileWsSupport wsSupport;
  private final QualityProfileChangeEventService qualityProfileChangeEventService;

  public RemoveProjectAction(DbClient dbClient, UserSession userSession, Languages languages, ComponentFinder componentFinder,
    QProfileWsSupport wsSupport, QualityProfileChangeEventService qualityProfileChangeEventService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.languages = languages;
    this.componentFinder = componentFinder;
    this.wsSupport = wsSupport;
    this.qualityProfileChangeEventService = qualityProfileChangeEventService;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction(ACTION_REMOVE_PROJECT)
      .setSince("5.2")
      .setDescription("Remove a project's association with a quality profile.<br> " +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Profiles'</li>" +
        "  <li>Edit right on the specified quality profile</li>" +
        "  <li>Administer right on the specified project</li>" +
        "</ul>")
      .setPost(true)
      .setHandler(this);
    QProfileReference.defineParams(action, languages);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = loadProject(dbSession, request);
      QProfileDto profile = wsSupport.getProfile(dbSession, QProfileReference.fromName(request));
      checkPermissions(profile, project);

      dbClient.qualityProfileDao().deleteProjectProfileAssociation(dbSession, project, profile);
      dbSession.commit();

      QProfileDto activatedProfile = null;

      // publish change for rules in the default quality profile
      QProfileDto defaultProfile = dbClient.qualityProfileDao().selectDefaultProfile(dbSession, profile.getLanguage());
      if (defaultProfile != null) {
        activatedProfile = defaultProfile;
      }

      qualityProfileChangeEventService.publishRuleActivationToSonarLintClients(project, activatedProfile, profile);

      response.noContent();
    }
  }

  private ProjectDto loadProject(DbSession dbSession, Request request) {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    return componentFinder.getProjectByKey(dbSession, projectKey);
  }

  private void checkPermissions(QProfileDto profile, ProjectDto project) {
    if (wsSupport.canAdministrate(profile) || userSession.hasEntityPermission(ProjectPermission.ADMIN, project)) {
      return;
    }

    throw insufficientPrivilegesException();
  }
}
