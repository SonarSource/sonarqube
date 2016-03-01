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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.issue.IssueType;
import org.sonar.core.util.Uuids;
import org.sonar.server.issue.IssueService;

public class SetTypeAction implements IssuesWsAction {

  public static final String ACTION = "set_type";

  private final IssueService issueService;
  private final OperationResponseWriter responseWriter;

  public SetTypeAction(IssueService issueService, OperationResponseWriter responseWriter) {
    this.issueService = issueService;
    this.responseWriter = responseWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION)
      .setDescription("Change type of issue, for instance from 'code smell' to 'bug'. Requires authentication and Browse permission on project.")
      .setSince("5.5")
      .setHandler(this)
      .setPost(true);

    action.createParam("issue")
      .setDescription("Key of the issue")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
    action.createParam("type")
      .setDescription("New type")
      .setRequired(true)
      .setPossibleValues(IssueType.ALL_NAMES);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String key = request.mandatoryParam("issue");
    issueService.setType(key, IssueType.valueOf(request.mandatoryParam("type")));

    responseWriter.write(key, request, response);
  }
}
