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
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.rule.RuleTypeMapper;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.pushapi.issues.IssueChangeEventService;
import org.sonar.server.user.UserSession;

import static org.sonar.api.server.rule.internal.ImpactMapper.convertToRuleSeverity;
import static org.sonar.api.server.rule.internal.ImpactMapper.convertToRuleType;
import static org.sonar.api.server.rule.internal.ImpactMapper.convertToSoftwareQuality;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonar.core.rule.ImpactSeverityMapper.mapImpactSeverity;
import static org.sonar.core.rule.RuleTypeMapper.toApiRuleType;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.server.common.ParamParsingUtils.parseImpact;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SET_SEVERITY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IMPACT;
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
        new Change("10.8", "Add 'impact' parameter to the request."),
        new Change("10.8", "Parameter 'severity' is now optional."),
        new Change("10.8", "This endpoint is not deprecated anymore."),
        new Change("10.4", "The response fields 'status' and 'resolution' are deprecated. Please use 'issueStatus' instead."),
        new Change("10.4", "Add 'issueStatus' field to the response."),
        new Change("10.2", "This endpoint is now deprecated."),
        new Change("10.2", "Add 'impacts', 'cleanCodeAttribute', 'cleanCodeAttributeCategory' fields to the response"),
        new Change("9.6", "Response field 'ruleDescriptionContextKey' added"),
        new Change("8.8", "The response field components.uuid is removed"),
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
      .setRequired(false)
      .setPossibleValues(Severity.ALL);
    action.createParam(PARAM_IMPACT)
      .setDescription("Override of impact severity for the rule. Cannot be used as the same time as 'severity'")
      .setRequired(false)
      .setExampleValue("impact=MAINTAINABILITY=HIGH");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    String issueKey = request.mandatoryParam(PARAM_ISSUE);
    String severity = request.param(PARAM_SEVERITY);
    String impact = request.param(PARAM_IMPACT);
    checkParams(severity, impact);

    try (DbSession session = dbClient.openSession(false)) {
      SearchResponseData preloadedSearchResponseData = setSeverity(session, issueKey, impact, severity);
      responseWriter.write(issueKey, preloadedSearchResponseData, request, response, true);
    }
  }

  private SearchResponseData setSeverity(DbSession session, String issueKey, @Nullable String impact, @Nullable String severity) {
    IssueDto issueDto = issueFinder.getByKey(session, issueKey);
    DefaultIssue issue = issueDto.toDefaultIssue();
    userSession.checkComponentUuidPermission(ISSUE_ADMIN, issue.projectUuid());

    IssueChangeContext context = issueChangeContextByUserBuilder(new Date(), userSession.getUuid()).withRefreshMeasures().build();
    SearchResponseData response;
    if (severity != null) {
      response = setManualSeverity(session, issue, issueDto, severity, context);
    } else {
      response = setManualImpact(session, issue, issueDto, impact, context);
    }
    return response;
  }

  private SearchResponseData setManualImpact(DbSession session, DefaultIssue issue, IssueDto issueDto, String impact,
    IssueChangeContext context) {
    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> effectiveImpacts = issueDto.getEffectiveImpacts();
    Pair<SoftwareQuality, org.sonar.api.issue.impact.Severity> manualImpact = parseImpact(impact);

    org.sonar.api.issue.impact.Severity manualImpactSeverity = manualImpact.getValue();
    SoftwareQuality softwareQuality = manualImpact.getKey();

    if (!effectiveImpacts.containsKey(softwareQuality)) {
      throw new IllegalArgumentException("Issue does not support impact " + softwareQuality);
    }
    createImpactsIfMissing(issue, effectiveImpacts);
    if (issueFieldsSetter.setImpactManualSeverity(issue, softwareQuality, manualImpactSeverity, context)) {
      String manualSeverity = null;
      boolean severityHasChanged = false;
      if (convertToRuleType(softwareQuality) == RuleTypeMapper.toApiRuleType(issue.type())) {
        manualSeverity = convertToRuleSeverity(manualImpactSeverity);
        severityHasChanged = issueFieldsSetter.setManualSeverity(issue, manualSeverity, context);
      }
      BranchDto branch = issueUpdater.getBranch(session, issue);
      SearchResponseData response = issueUpdater.saveIssueAndPreloadSearchResponseData(session, issueDto, issue, context, branch);
      if (branch.getBranchType().equals(BRANCH) && response.getComponentByUuid(issue.projectUuid()) != null) {
        issueChangeEventService.distributeIssueChangeEvent(issue, severityHasChanged ? manualSeverity : null, Map.of(softwareQuality, manualImpactSeverity), null, null,
          branch, getProjectKey(issue, response));
      }
      return response;
    }
    return new SearchResponseData(issueDto);
  }

  private SearchResponseData setManualSeverity(DbSession session, DefaultIssue issue, IssueDto issueDto, String severity,
    IssueChangeContext context) {
    if (issueFieldsSetter.setManualSeverity(issue, severity, context)) {
      SoftwareQuality softwareQuality = convertToSoftwareQuality(toApiRuleType(issue.type()));
      boolean impactHasChanged = false;
      if (issueDto.getEffectiveImpacts().containsKey(softwareQuality)) {
        createImpactsIfMissing(issue, issueDto.getEffectiveImpacts());
        impactHasChanged = issueFieldsSetter.setImpactManualSeverity(issue, softwareQuality, mapImpactSeverity(severity), context);
      }
      BranchDto branch = issueUpdater.getBranch(session, issue);
      SearchResponseData response = issueUpdater.saveIssueAndPreloadSearchResponseData(session, issueDto, issue, context, branch);
      if (branch.getBranchType().equals(BRANCH) && response.getComponentByUuid(issue.projectUuid()) != null) {
        issueChangeEventService.distributeIssueChangeEvent(issue, severity, impactHasChanged ? Map.of(softwareQuality, mapImpactSeverity(severity)) : Map.of(), null, null,
          branch, getProjectKey(issue, response));
      }
      return response;
    }
    return new SearchResponseData(issueDto);
  }

  private static String getProjectKey(DefaultIssue issue, SearchResponseData response) {
    ComponentDto componentByUuid = response.getComponentByUuid(issue.projectUuid());
    if (componentByUuid == null) {
      throw new IllegalStateException("Component with uuid " + issue.projectUuid() + " not found");
    }
    return componentByUuid.getKey();
  }

  private static void createImpactsIfMissing(DefaultIssue issue, Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> effectiveImpacts) {
    if (issue.impacts().isEmpty()) {
      issue.replaceImpacts(effectiveImpacts);
      issue.setChanged(true);
    }
  }

  private static void checkParams(@Nullable String severity, @Nullable String impact) {
    if (severity != null && impact != null) {
      throw new IllegalArgumentException("Parameters 'severity' and 'impact' cannot be used at the same time");
    } else if (severity == null && impact == null) {
      throw new IllegalArgumentException("One of the parameters 'severity' or 'impact' must be provided");
    }
  }
}
