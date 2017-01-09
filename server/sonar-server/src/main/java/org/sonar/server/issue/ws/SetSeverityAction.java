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

import com.google.common.io.Resources;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.Uuids;
import org.sonar.server.issue.IssueService;

import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SET_SEVERITY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEVERITY;

public class SetSeverityAction implements IssuesWsAction {

  private final IssueService issueService;
  private final OperationResponseWriter responseWriter;

  public SetSeverityAction(IssueService issueService, OperationResponseWriter responseWriter) {
    this.issueService = issueService;
    this.responseWriter = responseWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_SET_SEVERITY)
      .setDescription("Change severity. Requires authentication and Browse permission on project")
      .setSince("3.6")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "set_severity-example.json"))
      .setPost(true);

    action.createParam(PARAM_ISSUE)
      .setDescription("Issue key")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
    action.createParam(PARAM_SEVERITY)
      .setDescription("New severity")
      .setRequired(true)
      .setPossibleValues(Severity.ALL);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String key = request.mandatoryParam(PARAM_ISSUE);
    issueService.setSeverity(key, request.mandatoryParam(PARAM_SEVERITY));

    responseWriter.write(key, request, response);
  }
}
