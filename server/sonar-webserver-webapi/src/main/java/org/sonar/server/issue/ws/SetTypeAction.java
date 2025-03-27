/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.EnumSet;
import java.util.Map;
import org.sonar.core.rule.RuleType;
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
import org.sonar.db.component.BranchDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.pushapi.issues.IssueChangeEventService;
import org.sonar.server.user.UserSession;

import static org.sonar.db.permission.ProjectPermission.ISSUE_ADMIN;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SET_TYPE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TYPE;

public class SetTypeAction implements IssuesWsAction {
  private static final EnumSet<RuleType> ALL_RULE_TYPES_EXCEPT_SECURITY_HOTSPOTS = EnumSet.complementOf(EnumSet.of(RuleType.SECURITY_HOTSPOT));

  private final UserSession userSession;
  private final DbClient dbClient;
  private final IssueChangeEventService issueChangeEventService;
  private final IssueFinder issueFinder;
  private final IssueFieldsSetter issueFieldsSetter;
  private final IssueUpdater issueUpdater;
  private final OperationResponseWriter responseWriter;
  private final System2 system2;

  public SetTypeAction(UserSession userSession, DbClient dbClient, IssueChangeEventService issueChangeEventService, IssueFinder issueFinder,
    IssueFieldsSetter issueFieldsSetter, IssueUpdater issueUpdater, OperationResponseWriter responseWriter, System2 system2) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.issueChangeEventService = issueChangeEventService;
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
        new Change("10.4", "The response fields 'status' and 'resolution' are deprecated. Please use 'issueStatus' instead."),
        new Change("10.4", "Add 'issueStatus' field to the response."),
        new Change("10.2", "Add 'impacts', 'cleanCodeAttribute', 'cleanCodeAttributeCategory' fields to the response"),
        new Change("10.2", "This endpoint is now deprecated."),
        new Change("9.6", "Response field 'ruleDescriptionContextKey' added"),
        new Change("8.8", "The response field components.uuid is removed"),
        new Change("6.5", "the database ids of the components are removed from the response"),
        new Change("6.5", "the response field components.uuid is deprecated. Use components.key instead."))
      .setHandler(this)
      .setDeprecatedSince("10.2")
      .setResponseExample(Resources.getResource(this.getClass(), "set_type-example.json"))
      .setPost(true);

    action.createParam(PARAM_ISSUE)
      .setDescription("Issue key")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
    action.createParam(PARAM_TYPE)
      .setDescription("New type")
      .setRequired(true)
      .setPossibleValues(ALL_RULE_TYPES_EXCEPT_SECURITY_HOTSPOTS);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    String issueKey = request.mandatoryParam(PARAM_ISSUE);
    RuleType ruleType = RuleType.valueOf(request.mandatoryParam(PARAM_TYPE));
    try (DbSession session = dbClient.openSession(false)) {
      SearchResponseData preloadedSearchResponseData = setType(session, issueKey, ruleType);
      responseWriter.write(issueKey, preloadedSearchResponseData, request, response, true);
    }
  }

  private SearchResponseData setType(DbSession session, String issueKey, RuleType ruleType) {
    IssueDto issueDto = issueFinder.getByKey(session, issueKey);
    DefaultIssue issue = issueDto.toDefaultIssue();

    userSession.checkComponentUuidPermission(ISSUE_ADMIN, issue.projectUuid());

    IssueChangeContext context = issueChangeContextByUserBuilder(new Date(system2.now()), userSession.getUuid()).withRefreshMeasures().build();
    if (issueFieldsSetter.setType(issue, ruleType, context)) {
      BranchDto branch = issueUpdater.getBranch(session, issue);
      SearchResponseData response = issueUpdater.saveIssueAndPreloadSearchResponseData(session, issueDto, issue, context, branch);
      if (branch.getBranchType().equals(BRANCH) && response.getComponentByUuid(issue.projectUuid()) != null) {
        issueChangeEventService.distributeIssueChangeEvent(issue, null, Map.of(), ruleType.name(), null, branch,
          response.getComponentByUuid(issue.projectUuid()).getKey());
      }
      return response;
    }
    return new SearchResponseData(issueDto);
  }

}
