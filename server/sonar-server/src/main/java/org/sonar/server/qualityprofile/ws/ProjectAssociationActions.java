/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualityprofile.ws;

import com.google.common.base.Preconditions;
import org.sonar.api.ServerSide;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.server.component.ComponentService;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.QProfile;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.qualityprofile.QProfileProjectOperations;
import org.sonar.server.user.UserSession;

import static org.apache.commons.lang.StringUtils.isEmpty;

@ServerSide
public class ProjectAssociationActions {

  private static final String PARAM_LANGUAGE = "language";
  private static final String PARAM_PROFILE_NAME = "profileName";
  private static final String PARAM_PROFILE_KEY = "profileKey";
  private static final String PARAM_PROJECT_KEY = "projectKey";
  private static final String PARAM_PROJECT_UUID = "projectUuid";

  private final QProfileProjectOperations profileProjectOperations;
  private final QProfileLookup profileLookup;
  private final ComponentService componentService;
  private final Languages languages;

  public ProjectAssociationActions(QProfileProjectOperations profileProjectOperations, QProfileLookup profileLookup, ComponentService componentService, Languages languages) {
    this.profileProjectOperations = profileProjectOperations;
    this.profileLookup = profileLookup;
    this.componentService = componentService;
    this.languages = languages;
  }

  void define(WebService.NewController controller) {
    NewAction addProject = controller.createAction("add_project")
      .setSince("5.2")
      .setDescription("Associate a project with a quality profile.")
      .setHandler(new AssociationHandler(profileLookup, componentService) {
        @Override
        protected void changeAssociation(String profileKey, String projectUuid, UserSession userSession) {
          profileProjectOperations.addProject(profileKey, projectUuid, userSession);
        }
      });
    setCommonAttributes(addProject);

    NewAction removeProject = controller.createAction("remove_project")
      .setSince("5.2")
      .setDescription("Remove a project's association with a quality profile.")
      .setHandler(new AssociationHandler(profileLookup, componentService) {
        @Override
        protected void changeAssociation(String profileKey, String projectUuid, UserSession userSession) {
          profileProjectOperations.removeProject(profileKey, projectUuid, userSession);
        }
      });
    setCommonAttributes(removeProject);
  }

  private void setCommonAttributes(NewAction action) {
    action.setPost(true);
    action.createParam(PARAM_PROJECT_UUID)
      .setDescription("A project UUID. Either this parameter, or projectKey must be set.")
      .setExampleValue("69e57151-be0d-4157-adff-c06741d88879");
    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("A project key. Either this parameter, or projectUuid must be set.")
      .setExampleValue("org.codehaus.sonar:sonar");
    action.createParam(PARAM_PROFILE_KEY)
      .setDescription("A quality profile key. Either this parameter, or a combination of profileName + language must be set.")
      .setExampleValue("sonar-way-java-12345");
    action.createParam(PARAM_PROFILE_NAME)
      .setDescription("A quality profile name. If this parameter is set, profileKey must not be set and language must be set to disambiguate.")
      .setExampleValue("Soanr way");
    action.createParam(PARAM_LANGUAGE)
      .setDescription("A quality profile language. If this parameter is set, profileKey must not be set and profileName must be set to disambiguate.")
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages))
      .setExampleValue("js");
  }

  private abstract static class AssociationHandler implements RequestHandler {

    private final QProfileLookup profileLookup;
    private final ComponentService componentService;

    public AssociationHandler(QProfileLookup profileLookup, ComponentService componentService) {
      this.profileLookup = profileLookup;
      this.componentService = componentService;
    }

    protected abstract void changeAssociation(String profileKey, String projectUuid, UserSession userSession);

    @Override
    public void handle(Request request, Response response) throws Exception {
      String language = request.param(PARAM_LANGUAGE);
      String profileName = request.param(PARAM_PROFILE_NAME);
      String profileKey = request.param(PARAM_PROFILE_KEY);
      String projectKey = request.param(PARAM_PROJECT_KEY);
      String projectUuid = request.param(PARAM_PROJECT_UUID);

      Preconditions.checkArgument(
        (!isEmpty(language) && !isEmpty(profileName)) ^ !isEmpty(profileKey), "Either profileKey or profileName + language must be set");
      Preconditions.checkArgument(!isEmpty(projectKey) ^ !isEmpty(projectUuid), "Either projectKey or projectUuid must be set");

      if (profileKey == null) {
        profileKey = getProfileKeyFromLanguageAndName(language, profileName);
      }

      if (projectUuid == null) {
        projectUuid = componentService.getByKey(projectKey).uuid();
      }

      changeAssociation(profileKey, projectUuid, UserSession.get());
      response.noContent();
    }

    private String getProfileKeyFromLanguageAndName(String language, String profileName) {
      QProfile profile = profileLookup.profile(profileName, language);
      if (profile == null) {
        throw new NotFoundException(String.format("Unable to find a profile for language '%s' with name '%s'", language, profileName));
      }
      return profile.key();
    }
  }
}
