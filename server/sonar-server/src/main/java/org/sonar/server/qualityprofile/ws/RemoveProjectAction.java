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
package org.sonar.server.qualityprofile.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.server.qualityprofile.QProfileProjectOperations;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.qualityprofile.RemoveProjectRequest;

import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_REMOVE_PROJECT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_UUID;

public class RemoveProjectAction implements QProfileWsAction {

  private final ProjectAssociationParameters projectAssociationParameters;
  private final ProjectAssociationFinder projectAssociationFinder;
  private final QProfileProjectOperations profileProjectOperations;
  private final UserSession userSession;

  public RemoveProjectAction(ProjectAssociationParameters projectAssociationParameters, ProjectAssociationFinder projectAssociationFinder,
    QProfileProjectOperations profileProjectOperations, UserSession userSession) {
    this.projectAssociationParameters = projectAssociationParameters;
    this.projectAssociationFinder = projectAssociationFinder;
    this.profileProjectOperations = profileProjectOperations;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction(ACTION_REMOVE_PROJECT)
      .setSince("5.2")
      .setDescription("Remove a project's association with a quality profile.")
      .setHandler(this);
    projectAssociationParameters.addParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    RemoveProjectRequest removeProjectRequest = toWsRequest(request);
    String profileKey = projectAssociationFinder.getProfileKey(removeProjectRequest.getLanguage(), removeProjectRequest.getProfileName(), removeProjectRequest.getProfileKey());
    String projectUuid = projectAssociationFinder.getProjectUuid(removeProjectRequest.getProjectKey(), removeProjectRequest.getProjectUuid());
    profileProjectOperations.removeProject(profileKey, projectUuid, userSession);
    response.noContent();
  }

  private static RemoveProjectRequest toWsRequest(Request request) {
    return RemoveProjectRequest.builder()
      .setLanguage(request.param(PARAM_LANGUAGE))
      .setProfileName(request.param(PARAM_PROFILE_NAME))
      .setProfileKey(request.param(PARAM_PROFILE_KEY))
      .setProjectKey(request.param(PARAM_PROJECT_KEY))
      .setProjectUuid(request.param(PARAM_PROJECT_UUID))
      .build();
  }

}
