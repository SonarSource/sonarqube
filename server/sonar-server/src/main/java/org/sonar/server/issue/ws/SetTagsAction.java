/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.issue.ws;

import com.google.common.base.MoreObjects;
import com.google.common.io.Resources;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.issue.IssueUpdater;
import org.sonar.server.user.UserSession;

import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SET_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TAGS;

/**
 * Set tags on an issue
 */
public class SetTagsAction implements IssuesWsAction {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final IssueFinder issueFinder;
  private final IssueFieldsSetter issueFieldsSetter;
  private final IssueUpdater issueUpdater;
  private final OperationResponseWriter responseWriter;

  public SetTagsAction(UserSession userSession, DbClient dbClient, IssueFinder issueFinder, IssueFieldsSetter issueFieldsSetter, IssueUpdater issueUpdater,
    OperationResponseWriter responseWriter) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.issueFinder = issueFinder;
    this.issueFieldsSetter = issueFieldsSetter;
    this.issueUpdater = issueUpdater;
    this.responseWriter = responseWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction(ACTION_SET_TAGS)
      .setPost(true)
      .setSince("5.1")
      .setDescription("Set tags on an issue. <br/>" +
        "Requires authentication and Browse permission on project")
      .setChangelog(
        new Change("6.5", "the database ids of the components are removed from the response"),
        new Change("6.5", "the response field components.uuid is deprecated. Use components.key instead."),
        new Change("6.4", "response contains issue information instead of list of tags"))
      .setResponseExample(Resources.getResource(this.getClass(), "set_tags-example.json"))
      .setHandler(this);
    action.createParam(PARAM_ISSUE)
      .setDescription("Issue key")
      .setSince("6.3")
      .setDeprecatedKey("key", "6.3")
      .setExampleValue(Uuids.UUID_EXAMPLE_01)
      .setRequired(true);
    action.createParam(PARAM_TAGS)
      .setDescription("Comma-separated list of tags. All tags are removed if parameter is empty or not set.")
      .setExampleValue("security,cwe,misra-c");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String key = request.mandatoryParam(PARAM_ISSUE);
    List<String> tags = MoreObjects.firstNonNull(request.paramAsStrings(PARAM_TAGS), Collections.<String>emptyList());
    SearchResponseData preloadedSearchResponseData = setTags(key, tags);
    responseWriter.write(key, preloadedSearchResponseData, request, response);
  }

  private SearchResponseData setTags(String issueKey, List<String> tags) {
    userSession.checkLoggedIn();
    try (DbSession session = dbClient.openSession(false)) {
      IssueDto issueDto = issueFinder.getByKey(session, issueKey);
      DefaultIssue issue = issueDto.toDefaultIssue();
      IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.getUuid());
      if (issueFieldsSetter.setTags(issue, tags, context)) {
        return issueUpdater.saveIssueAndPreloadSearchResponseData(session, issue, context, null, false);
      }
      return new SearchResponseData(issueDto);
    }
  }

}
