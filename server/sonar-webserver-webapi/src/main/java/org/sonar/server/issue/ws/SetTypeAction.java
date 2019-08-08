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

import com.google.common.io.Resources;
import java.util.Date;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.user.UserSession;

import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SET_TYPE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TYPE;

public class SetTypeAction implements IssuesWsAction {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final IssueFinder issueFinder;
  private final IssueFieldsSetter issueFieldsSetter;
  private final IssueUpdater issueUpdater;
  private final OperationResponseWriter responseWriter;
  private final System2 system2;

  public SetTypeAction(UserSession userSession, DbClient dbClient, IssueFinder issueFinder, IssueFieldsSetter issueFieldsSetter, IssueUpdater issueUpdater,
    OperationResponseWriter responseWriter, System2 system2) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.issueFinder = issueFinder;
    this.issueFieldsSetter = issueFieldsSetter;
    this.issueUpdater = issueUpdater;
    this.responseWriter = responseWriter;
    this.system2 = system2;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_SET_TYPE)
      .setDescription("Change type of issue, for instance from 'code smell' to 'bug'.<br/>" +
        "Requires the following permissions:" +
        "<ul>" +
        "  <li>'Authentication'</li>" +
        "  <li>'Browse' rights on project of the specified issue</li>" +
        "  <li>'Administer Issues' rights on project of the specified issue</li>" +
        "</ul>")
      .setSince("5.5")
      .setChangelog(
        new Change("6.5", "the database ids of the components are removed from the response"),
        new Change("6.5", "the response field components.uuid is deprecated. Use components.key instead."))
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "set_type-example.json"))
      .setPost(true);

    action.createParam(PARAM_ISSUE)
      .setDescription("Issue key")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
    action.createParam(PARAM_TYPE)
      .setDescription("New type")
      .setRequired(true)
      .setPossibleValues(RuleType.names());
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    String issueKey = request.mandatoryParam(PARAM_ISSUE);
    RuleType ruleType = RuleType.valueOf(request.mandatoryParam(PARAM_TYPE));
    try (DbSession session = dbClient.openSession(false)) {
      SearchResponseData preloadedSearchResponseData = setType(session, issueKey, ruleType);
      responseWriter.write(issueKey, preloadedSearchResponseData, request, response);
    }
  }

  private SearchResponseData setType(DbSession session, String issueKey, RuleType ruleType) {
    IssueDto issueDto = issueFinder.getByKey(session, issueKey);
    DefaultIssue issue = issueDto.toDefaultIssue();
    if (issue.isFromHotspot()) {
      throw new IllegalArgumentException("Changing type of a security hotspot is not permitted");
    }
    userSession.checkComponentUuidPermission(ISSUE_ADMIN, issue.projectUuid());

    IssueChangeContext context = IssueChangeContext.createUser(new Date(system2.now()), userSession.getUuid());
    if (issueFieldsSetter.setType(issue, ruleType, context)) {
      return issueUpdater.saveIssueAndPreloadSearchResponseData(session, issue, context, true);
    }
    return new SearchResponseData(issueDto);
  }

}
