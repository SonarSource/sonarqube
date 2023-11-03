/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.issue.ws.anticipatedtransition;

import java.io.BufferedReader;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.issue.ws.IssuesWsAction;
import org.sonarqube.ws.client.issue.IssuesWsParameters;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;

public class AnticipatedTransitionsAction implements IssuesWsAction {

  private static final String PARAM_PROJECT_KEY = "projectKey";
  private final AnticipatedTransitionsActionValidator validator;
  private final AnticipatedTransitionHandler anticipatedTransitionHandler;

  public AnticipatedTransitionsAction(AnticipatedTransitionsActionValidator validator, AnticipatedTransitionHandler anticipatedTransitionHandler) {
    this.validator = validator;
    this.anticipatedTransitionHandler = anticipatedTransitionHandler;
  }


  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction(IssuesWsParameters.ACTION_ANTICIPATED_TRANSITIONS)
      .setChangelog(
        new Change("10.4", "Transition '%s' is now deprecated. Use '%s' instead.".formatted(DefaultTransitions.WONT_FIX, DefaultTransitions.ACCEPT)),
        new Change("10.4", "Transition '%s' has been added.".formatted(DefaultTransitions.ACCEPT))
      )
      .setDescription("""
      Receive a list of anticipated transitions that can be applied to not yet discovered issues on a specific project.<br>
      Requires the following permission: 'Administer Issues' on the specified project.<br>
      Only <code>falsepositive</code> and <code>wontfix</code> transitions are supported.<br>
      Upon successful execution, the HTTP status code returned is 202 (Accepted).<br><br>
      Request example:
      <pre><code>[
        {
          "ruleKey": "squid:S0001",
          "issueMessage": "issueMessage1",
          "filePath": "filePath1",
          "line": 1,
          "lineHash": "lineHash1",
          "transition": "falsepositive",
          "comment": "comment1"
        },
        {
          "ruleKey": "squid:S0002",
          "issueMessage": "issueMessage2",
          "filePath": "filePath2",
          "line": 2,
          "lineHash": "lineHash2",
          "transition": "wontfix",
          "comment": "comment2"
        }
      ]</code></pre>""")
      .setSince("10.2")
      .setHandler(this)
      .setInternal(true)
      .setPost(true);

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project key")
      .setRequired(true)
      .setDescription("The key of the project")
      .setExampleValue("my_project")
      .setSince("10.2");

  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    // validation
    String userUuid = validator.validateUserLoggedIn();
    ProjectDto projectDto = validator.validateProjectKey(request.mandatoryParam(PARAM_PROJECT_KEY));
    validator.validateUserHasAdministerIssuesPermission(projectDto.getUuid());

    String requestBody = getRequestBody(request);
    anticipatedTransitionHandler.handleRequestBody(requestBody, userUuid, projectDto);

    response.stream().setStatus(HTTP_ACCEPTED);
  }

  private static String getRequestBody(Request request) {
    BufferedReader reader = request.getReader();
    StringBuilder sb = new StringBuilder();
    reader.lines().forEach(sb::append);
    return sb.toString();
  }
}
