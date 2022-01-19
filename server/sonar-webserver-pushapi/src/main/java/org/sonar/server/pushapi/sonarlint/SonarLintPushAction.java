/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.server.pushapi.sonarlint;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.pushapi.ServerPushAction;

public class SonarLintPushAction implements ServerPushAction {

  private static final Logger LOGGER = Loggers.get(SonarLintPushAction.class);

  private static final String PROJECT_PARAM_KEY = "projectKeys";
  private static final String LANGUAGE_PARAM_KEY = "languages";

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("sonarlint_events")
      .setInternal(true)
      .setDescription("Endpoint for listening to server side events. Currently it notifies listener about change to activation of a rule")
      .setSince("9.4")
      .setHandler(this);

    action
      .createParam(PROJECT_PARAM_KEY)
      .setDescription("Comma-separated list of projects keys for which events will be delivered")
      .setRequired(true)
      .setExampleValue("example-project-key,example-project-key2");

    action
      .createParam(LANGUAGE_PARAM_KEY)
      .setDescription("Comma-separated list of languages for which events will be delivered")
      .setRequired(true)
      .setExampleValue("java,cobol");
  }

  @Override
  public void handle(Request request, Response response) {
    String projectKeys = request.getParam(PROJECT_PARAM_KEY).getValue();
    String languages = request.getParam(LANGUAGE_PARAM_KEY).getValue();

    //to remove later
    LOGGER.debug(projectKeys != null ? projectKeys : "");
    LOGGER.debug(languages != null ? languages : "");

    response.noContent();
  }
}
