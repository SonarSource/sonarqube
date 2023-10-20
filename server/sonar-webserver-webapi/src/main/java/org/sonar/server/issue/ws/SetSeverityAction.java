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
package org.sonar.server.issue.ws;

import com.google.common.io.Resources;
import java.util.Date;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.pushapi.issues.IssueChangeEventService;
import org.sonar.server.user.UserSession;

import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SET_SEVERITY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEVERITY;

public class SetSeverityAction implements IssuesWsAction {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final IssueChangeEventService issueChangeEventService;
  private final IssueFinder issueFinder;
  private final IssueFieldsSetter issueFieldsSetter;
  private final IssueUpdater issueUpdater;
  private final OperationResponseWriter responseWriter;
  private final Logger logger = Loggers.get(SetSeverityAction.class);

  public SetSeverityAction(UserSession userSession, DbClient dbClient, IssueChangeEventService issueChangeEventService,
    IssueFinder issueFinder, IssueFieldsSetter issueFieldsSetter, IssueUpdater issueUpdater,
    OperationResponseWriter responseWriter) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.issueChangeEventService = issueChangeEventService;
    this.issueFinder = issueFinder;
    this.issueFieldsSetter = issueFieldsSetter;
    this.issueUpdater = issueUpdater;
    this.responseWriter = responseWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_SET_SEVERITY)
      .setDescription("Change severity.<br/>" +
        "Requires the following permissions:" +
        "<ul>" +
        "  <li>'Authentication'</li>" +
        "  <li>'Browse' rights on project of the specified issue</li>" +
        "  <li>'Administer Issues' rights on project of the specified issue</li>" +
        "</ul>")
      .setSince("3.6")
      .setChangelog(
        new Change("9.6", "Response field 'ruleDescriptionContextKey' added"),
        new Change("6.5", "the database ids of the components are removed from the response"),
        new Change("6.5", "the response field components.uuid is deprecated. Use components.key instead."))
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
    userSession.checkLoggedIn();
    String issueKey = request.mandatoryParam(PARAM_ISSUE);
    String severity = request.mandatoryParam(PARAM_SEVERITY);
    try (DbSession session = dbClient.openSession(false)) {
      SearchResponseData preloadedSearchResponseData = setType(session, issueKey, severity);
      responseWriter.write(issueKey, preloadedSearchResponseData, request, response);
    }
  }

  private SearchResponseData setType(DbSession session, String issueKey, String severity) {
    IssueDto issueDto = issueFinder.getByKey(session, issueKey);
    DefaultIssue issue = issueDto.toDefaultIssue();
    logger.info("Change Issue Severity:: from: {}, to: {}, project: {}, issueId: {}, user: {}",
            issueDto.getSeverity(), severity, issueDto.getProjectKey(), issueDto.getKee(), userSession.getLogin());
    userSession.checkComponentUuidPermission(ISSUE_ADMIN, issue.projectUuid());

    IssueChangeContext context = issueChangeContextByUserBuilder(new Date(), userSession.getUuid()).withRefreshMeasures().build();
    if (issueFieldsSetter.setManualSeverity(issue, severity, context)) {
      BranchDto branch = issueUpdater.getBranch(session, issue, issue.projectUuid());
      SearchResponseData response = issueUpdater.saveIssueAndPreloadSearchResponseData(session, issue, context, branch);

      if (branch.getBranchType().equals(BRANCH) && response.getComponentByUuid(issue.projectUuid()) != null) {
        issueChangeEventService.distributeIssueChangeEvent(issue, severity, null, null,
          branch, response.getComponentByUuid(issue.projectUuid()).getKey());
      }
      return response;
    }
    return new SearchResponseData(issueDto);
  }
}
