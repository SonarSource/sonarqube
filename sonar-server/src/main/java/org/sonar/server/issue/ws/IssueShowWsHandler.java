/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.sonar.api.issue.*;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueService;
import org.sonar.server.user.UserSession;

import java.util.Arrays;
import java.util.List;

public class IssueShowWsHandler implements RequestHandler {

  private final IssueFinder issueFinder;
  private final IssueService issueService;

  public IssueShowWsHandler(IssueFinder issueFinder, IssueService issueService) {
    this.issueFinder = issueFinder;
    this.issueService = issueService;
  }

  @Override
  public void handle(Request request, Response response) {
    String issueKey = request.requiredParam("key");
    IssueQueryResult queryResult = issueFinder.find(IssueQuery.builder().issueKeys(Arrays.asList(issueKey)).build());
    if (queryResult.issues().size() != 1) {
      throw new NotFoundException("Issue not found: " + issueKey);
    }
    Issue issue = queryResult.first();

    JsonWriter json = response.newJsonWriter();
    json.beginObject().name("issue").beginObject();

    writeIssue(queryResult, issue, json);
    writeTransitions(issue, json);
    writeComments(queryResult, issue, json);
    //TODO write component, changelog and available commands

    json.endObject().endObject().close();
  }

  private void writeComments(IssueQueryResult queryResult, Issue issue, JsonWriter json) {
    json.name("comments").beginArray();
    for (IssueComment comment : issue.comments()) {
      json
        .beginObject()
        .prop("key", comment.key())
        .prop("userLogin", comment.userLogin())
        .prop("userName", queryResult.user(comment.userLogin()).name())
          // TODO convert markdown to HTML
        .endObject();
    }
    json.endArray();
  }

  private void writeIssue(IssueQueryResult result, Issue issue, JsonWriter json) {
    json
      .prop("key", issue.key())
      .prop("line", issue.line())
      .prop("message", issue.message())
      .prop("resolution", issue.resolution())
      .prop("status", issue.status())
      .prop("status", issue.status())
      .prop("severity", issue.severity());
    // TODO to be completed
    if (issue.assignee() != null) {
      json
        .prop("assignee", issue.assignee())
        .prop("assigneeName", result.user(issue.assignee()).name());
    }
  }

  private void writeTransitions(Issue issue, JsonWriter json) {
    List<Transition> transitions = issueService.listTransitions(issue, UserSession.get());
    json.name("transitions").beginArray();
    for (Transition transition : transitions) {
      json.value(transition.key());
    }
    json.endArray();
  }
}
