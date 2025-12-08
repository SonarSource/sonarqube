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
import io.sonarcloud.compliancereports.dao.IssueStats;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
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
import org.sonar.db.report.IssueStatsByRuleKeyDaoImpl;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.pushapi.issues.IssueChangeEventService;
import org.sonar.server.user.UserSession;

import static io.sonarcloud.compliancereports.dao.AggregationType.PROJECT;
import static java.lang.String.format;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.rule.SeverityUtil.getOrdinalFromSeverity;
import static org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition.ACCEPT;
import static org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition.CONFIRM;
import static org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition.FALSE_POSITIVE;
import static org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition.REOPEN;
import static org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition.RESOLVE;
import static org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition.UNCONFIRM;
import static org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition.WONT_FIX;
import static org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowTransition.RESET_AS_TO_REVIEW;
import static org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowTransition.RESOLVE_AS_REVIEWED;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_DO_TRANSITION;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TRANSITION;

public class DoTransitionAction implements IssuesWsAction {

  private static final Set<String> REOPEN_TRANSITIONS = Set.of(
    REOPEN.getKey(),
    UNCONFIRM.getKey()
  );

  private final DbClient dbClient;
  private final UserSession userSession;
  private final IssueChangeEventService issueChangeEventService;
  private final IssueFinder issueFinder;
  private final IssueUpdater issueUpdater;
  private final TransitionService transitionService;
  private final OperationResponseWriter responseWriter;
  private final System2 system2;

  public DoTransitionAction(DbClient dbClient, UserSession userSession, IssueChangeEventService issueChangeEventService,
    IssueFinder issueFinder, IssueUpdater issueUpdater, TransitionService transitionService,
    OperationResponseWriter responseWriter, System2 system2) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.issueChangeEventService = issueChangeEventService;
    this.issueFinder = issueFinder;
    this.issueUpdater = issueUpdater;
    this.transitionService = transitionService;
    this.responseWriter = responseWriter;
    this.system2 = system2;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_DO_TRANSITION)
      .setDescription("""
        Do workflow transition on an issue. Requires authentication and Browse permission on project.<br/>
        The transitions '%s', '%s' and '%s' require the permission 'Administer Issues'.<br/>
        The transitions involving security hotspots require the permission 'Administer Security Hotspot'.
        """.formatted(ACCEPT, WONT_FIX, FALSE_POSITIVE))
      .setSince("3.6")
      .setChangelog(
        new Change("10.8", "The response fields 'severity' and 'type' are not deprecated anymore."),
        new Change("10.8", format("Possible values '%s' and '%s' for response field 'severity' of 'impacts' have been added.",
          Severity.INFO.name(), Severity.BLOCKER.name())),
        new Change("10.4",
          "The transitions '%s' and '%s' are deprecated. Please use '%s' instead. The transition '%s' is deprecated too. ".formatted(WONT_FIX, CONFIRM, ACCEPT, UNCONFIRM)),
        new Change("10.4", "Add transition '%s'.".formatted(ACCEPT)),
        new Change("10.4", "The response fields 'severity' and 'type' are deprecated. Please use 'impacts' instead."),
        new Change("10.4", "The response fields 'status' and 'resolution' are deprecated. Please use 'issueStatus' instead."),
        new Change("10.4", "Add 'issueStatus' field to the response."),
        new Change("10.2", "Add 'impacts', 'cleanCodeAttribute', 'cleanCodeAttributeCategory' fields to the response"),
        new Change("9.6", "Response field 'ruleDescriptionContextKey' added"),
        new Change("8.8", "The response field components.uuid is removed"),
        new Change("8.1", format("transitions '%s' and '%s' are no more supported", "setinreview", "openasvulnerability")),
        new Change("7.8", format("added '%s', %s, %s and %s transitions for security hotspots ", "setinreview", RESOLVE_AS_REVIEWED,
          "openasvulnerability", RESET_AS_TO_REVIEW)),
        new Change("7.3", "added transitions for security hotspots"),
        new Change("6.5", "the database ids of the components are removed from the response"),
        new Change("6.5", "the response field components.uuid is deprecated. Use components.key instead."))
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "do_transition-example.json"))
      .setPost(true);

    action.createParam(PARAM_ISSUE)
      .setDescription("Issue key")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
    action.createParam(PARAM_TRANSITION)
      .setDescription("Transition")
      .setRequired(true)
      .setPossibleValues(List.of(
        CONFIRM.getKey(),
        UNCONFIRM.getKey(),
        REOPEN.getKey(),
        RESOLVE.getKey(),
        FALSE_POSITIVE.getKey(),
        WONT_FIX.getKey(),
        RESOLVE_AS_REVIEWED.getKey(),
        RESET_AS_TO_REVIEW.getKey(),
        ACCEPT.getKey())
      );
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkLoggedIn();
    String issue = request.mandatoryParam(PARAM_ISSUE);
    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueDto issueDto = issueFinder.getByKey(dbSession, issue);
      SearchResponseData preloadedSearchResponseData = doTransition(dbSession, issueDto, request.mandatoryParam(PARAM_TRANSITION));
      responseWriter.write(issue, preloadedSearchResponseData, request, response, true);
    }
  }

  private SearchResponseData doTransition(DbSession session, IssueDto issueDto, String transitionKey) {
    DefaultIssue defaultIssue = issueDto.toDefaultIssue();
    IssueChangeContext context =
      issueChangeContextByUserBuilder(new Date(system2.now()), userSession.getUuid()).withRefreshMeasures().build();
    transitionService.checkTransitionPermission(transitionKey, defaultIssue);
    if (transitionService.doTransition(defaultIssue, context, transitionKey)) {
      BranchDto branch = issueUpdater.getBranch(session, defaultIssue);
      SearchResponseData response = issueUpdater.saveIssueAndPreloadSearchResponseData(session, issueDto, defaultIssue, context, branch);
      updateIssueStatsByRuleKey(branch, issueDto.getRuleKey(), defaultIssue, transitionKey);

      if (branch.getBranchType().equals(BRANCH) && response.getComponentByUuid(defaultIssue.projectUuid()) != null) {
        issueChangeEventService.distributeIssueChangeEvent(defaultIssue, null, Map.of(), null, transitionKey, branch,
          response.getComponentByUuid(defaultIssue.projectUuid()).getKey());
      }
      return response;
    }
    return new SearchResponseData(issueDto);
  }

  private void updateIssueStatsByRuleKey(BranchDto branchDto, RuleKey ruleKey, DefaultIssue issue, String transitionKey) {
    IssueStatsByRuleKeyDaoImpl dao = new IssueStatsByRuleKeyDaoImpl(dbClient);
    var issueStats = dao.getIssueStats(branchDto.getUuid(), PROJECT).stream()
      .filter(i -> i.ruleKey().equals(ruleKey.toString()))
      .findFirst();

    if (issueStats.isPresent()) {
      var updatedIssueStats = updateIssueStatsWithTransition(issueStats.get(), issue, transitionKey);
      dao.upsert(branchDto.getUuid(), PROJECT, updatedIssueStats);
    }
  }

  private static IssueStats updateIssueStatsWithTransition(IssueStats oldStats, DefaultIssue issue, String transitionKey) {
    int adjustment = REOPEN_TRANSITIONS.contains(transitionKey) ? 1 : -1;
    int newIssueCount = oldStats.issueCount() + adjustment;
    if (newIssueCount == 0) {
      return new IssueStats(oldStats.ruleKey(), newIssueCount, 1, 1, 0, 0);
    }

    int newRating = getOrdinalFromSeverity(Objects.requireNonNull(issue.severity())) + 1;
    int newMqrRating = issue.impacts().getOrDefault(SoftwareQuality.SECURITY, Severity.INFO).ordinal() + 1;
    return new IssueStats(oldStats.ruleKey(), newIssueCount, newRating, newMqrRating, 0, 0);
  }
}
