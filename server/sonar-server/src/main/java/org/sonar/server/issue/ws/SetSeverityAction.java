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
package org.sonar.server.issue.ws;

import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.issue.IssueService;

public class SetSeverityAction implements IssuesWsAction {

  public static final String ACTION = "set_severity";

  private final IssueService issueService;
  private final OperationResponseWriter responseWriter;

  public SetSeverityAction(IssueService issueService, OperationResponseWriter responseWriter) {
    this.issueService = issueService;
    this.responseWriter = responseWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION)
      .setDescription("Change severity. Requires authentication and Browse permission on project")
      .setSince("3.6")
      .setHandler(this)
      .setPost(true);

    action.createParam("issue")
      .setDescription("Key of the issue")
      .setRequired(true)
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    action.createParam("severity")
      .setDescription("New severity")
      .setRequired(true)
      .setPossibleValues(Severity.ALL);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String key = request.mandatoryParam("issue");
    issueService.setSeverity(key, request.mandatoryParam("severity"));

    responseWriter.write(key, request, response);
  }
}
