/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.user.UserSession;

import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SET_CODEFIX_STATUS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE_CODEFIX_STATUSES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;

/**
 * Sets the codefix_status field on an issue and saves it (DB + index updated via save path).
 * Used by codescanng when job status changes so the issue search facet reflects the change immediately.
 */
public class SetCodefixStatusAction implements IssuesWsAction {

  private static final Logger log = LoggerFactory.getLogger(SetCodefixStatusAction.class);
  private final DbClient dbClient;
  private final IssueFinder issueFinder;
  private final IssueUpdater issueUpdater;
  private final UserSession userSession;
  private final System2 system2;
  private static final Set<String> VALID_STATUS = new HashSet<>(Set.of("PENDING", "IN_PROGRESS", "FIX_GENERATED", "PULL_REQUEST_CREATED", "FAILED"));

  public SetCodefixStatusAction(DbClient dbClient, IssueFinder issueFinder, IssueUpdater issueUpdater,
    UserSession userSession, System2 system2) {
    this.dbClient = dbClient;
    this.issueFinder = issueFinder;
    this.issueUpdater = issueUpdater;
    this.userSession = userSession;
    this.system2 = system2;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_SET_CODEFIX_STATUS)
      .setDescription("Set the AI codefix status on an issue. Updates the issue and its index so search facets stay in sync. " +
        "Typically called by Codescanng when a codefix job status changes. Requires authentication.")
      .setSince("10.8")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_ISSUE)
      .setDescription("Issue key")
      .setRequired(true);
    action.createParam(PARAM_ISSUE_CODEFIX_STATUSES)
      .setDescription("Codefix status (e.g. PENDING, IN_PROGRESS, FIX_GENERATED, PULL_REQUEST_CREATED, FAILED, AVAILABLE)")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkLoggedIn();
    String issueKey = request.mandatoryParam(PARAM_ISSUE);
    String codefixStatus = request.mandatoryParam(PARAM_ISSUE_CODEFIX_STATUSES);
    if (!VALID_STATUS.contains(codefixStatus)) {
      log.error("Failed to update issue status for {}: invalid status value '{}'", issueKey, codefixStatus);
      return;
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueDto issueDto = issueFinder.getByKey(dbSession, issueKey);
      DefaultIssue defaultIssue = issueDto.toDefaultIssue();
      defaultIssue.setCodefixStatus(codefixStatus);
      defaultIssue.setUpdateDate(new Date(system2.now()));
      defaultIssue.setChanged(true);

      IssueChangeContext context = issueChangeContextByUserBuilder(new Date(system2.now()), userSession.getUuid()).build();
      BranchDto branch = issueUpdater.getBranch(dbSession, defaultIssue);
      issueUpdater.saveIssueAndPreloadSearchResponseData(dbSession, issueDto, defaultIssue, context, branch);
    }
    response.noContent();
  }
}
