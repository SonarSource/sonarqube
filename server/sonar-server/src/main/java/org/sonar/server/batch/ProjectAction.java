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

package org.sonar.server.batch;

import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.server.plugins.MimeTypes;

public class ProjectAction implements BatchWsAction {

  private static final String PARAM_KEY = "key";
  private static final String PARAM_PROFILE = "profile";
  private static final String PARAM_PREVIEW = "preview";

  private final ProjectDataLoader projectDataLoader;

  public ProjectAction(ProjectDataLoader projectDataLoader) {
    this.projectDataLoader = projectDataLoader;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("project")
      .setDescription("Return project repository")
      .setSince("4.5")
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Project or module key")
      .setExampleValue("org.codehaus.sonar:sonar");

    action
      .createParam(PARAM_PROFILE)
      .setDescription("Profile name")
      .setExampleValue("SonarQube Way");

    action
      .createParam(PARAM_PREVIEW)
      .setDescription("Preview mode or not")
      .setDefaultValue(false)
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ProjectRepositories data = projectDataLoader.load(ProjectDataQuery.create()
      .setModuleKey(request.mandatoryParam(PARAM_KEY))
      .setProfileName(request.param(PARAM_PROFILE))
      .setPreview(request.mandatoryParamAsBoolean(PARAM_PREVIEW)));
    response.stream().setMediaType(MimeTypes.JSON);
    IOUtils.write(data.toJson(), response.stream().output());
  }

}
