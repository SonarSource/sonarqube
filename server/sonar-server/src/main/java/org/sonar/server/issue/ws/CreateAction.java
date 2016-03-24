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
package org.sonar.server.issue.ws;

import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class CreateAction implements IssuesWsAction {

  public static final String ACTION = "create";

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION)
      .setDescription("Deprecated web service to do create a manual issue. Manual issue feature has been removed in 5.5. This web service has no effect.")
      .setSince("3.6")
      .setDeprecatedSince("5.5")
      .setHandler(this)
      .setPost(true);

    action.createParam("component")
      .setDescription("Key of the component on which to log the issue")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action.createParam("rule")
      .setDescription("Manual rule key")
      .setRequired(true)
      .setExampleValue("manual:performance");
    action.createParam("severity")
      .setDescription("Severity of the issue")
      .setExampleValue(Severity.BLOCKER + "," + Severity.CRITICAL)
      .setPossibleValues(Severity.ALL);
    action.createParam("line")
      .setDescription("Line on which to log the issue. " +
        "If no line is specified, the issue is attached to the component and not to a specific line")
      .setExampleValue("15");
    action.createParam("message")
      .setDescription("Description of the issue")
      .setExampleValue("blabla...");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    response.noContent();
  }
}
