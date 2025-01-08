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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueQueryParams;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.TaintChecker;
import org.sonar.server.issue.ws.pull.PullActionProtobufObjectGenerator;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common.RuleType;

import static java.lang.Boolean.parseBoolean;
import static java.util.Optional.ofNullable;
import static org.sonarqube.ws.WsUtils.checkArgument;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_PULL;

public class PullAction extends BasePullAction implements IssuesWsAction {
  private static final String ISSUE_TYPE = "issues";
  private static final String REPOSITORY_EXAMPLE = "java";
  private static final String RESOURCE_EXAMPLE = "pull-example.proto";
  private static final String SINCE_VERSION = "9.5";

  private final DbClient dbClient;
  private final TaintChecker taintChecker;

  public PullAction(System2 system2, ComponentFinder componentFinder, DbClient dbClient, UserSession userSession,
    PullActionProtobufObjectGenerator protobufObjectGenerator, TaintChecker taintChecker) {
    super(system2, componentFinder, dbClient, userSession, protobufObjectGenerator, ACTION_PULL,
      ISSUE_TYPE, REPOSITORY_EXAMPLE, SINCE_VERSION, RESOURCE_EXAMPLE);
    this.dbClient = dbClient;
    this.taintChecker = taintChecker;
  }

  @Override
  protected void additionalParams(WebService.NewAction action) {
    action.createParam(RULE_REPOSITORIES_PARAM)
      .setDescription("Comma separated list of rule repositories. If not present all issues regardless of" +
        " their rule repository are returned.")
      .setExampleValue(repositoryExample);

    action.createParam(RESOLVED_ONLY_PARAM)
      .setDescription("If true only issues with resolved status are returned")
      .setExampleValue("true");
  }

  @Override
  protected void processAdditionalParams(Request request, BasePullRequest wsRequest) {
    boolean resolvedOnly = parseBoolean(request.param(RESOLVED_ONLY_PARAM));

    List<String> ruleRepositories = request.paramAsStrings(RULE_REPOSITORIES_PARAM);
    if (ruleRepositories != null && !ruleRepositories.isEmpty()) {
      validateRuleRepositories(ruleRepositories);
    }
    wsRequest
      .repositories(ruleRepositories)
      .resolvedOnly(resolvedOnly);
  }

  @Override
  protected Set<String> getIssueKeysSnapshot(IssueQueryParams issueQueryParams, int page) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<Long> changedSinceDate = ofNullable(issueQueryParams.getChangedSince());

      if (changedSinceDate.isPresent()) {
        return dbClient.issueDao().selectIssueKeysByComponentUuidAndChangedSinceDate(dbSession, issueQueryParams.getBranchUuid(),
          changedSinceDate.get(), issueQueryParams.getRuleRepositories(), taintChecker.getTaintRepositories(),
          issueQueryParams.getLanguages(), page);
      }

      return dbClient.issueDao().selectIssueKeysByComponentUuid(dbSession, issueQueryParams.getBranchUuid(),
        issueQueryParams.getRuleRepositories(), taintChecker.getTaintRepositories(),
        issueQueryParams.getLanguages(), page);
    }
  }

  @Override
  protected IssueQueryParams initializeQueryParams(BranchDto branchDto, @Nullable List<String> languages,
    @Nullable List<String> ruleRepositories, boolean resolvedOnly, @Nullable Long changedSince) {
    return new IssueQueryParams(branchDto.getUuid(), languages, ruleRepositories, taintChecker.getTaintRepositories(), resolvedOnly, changedSince);
  }

  @Override
  protected boolean filterNonClosedIssues(IssueDto issueDto, IssueQueryParams queryParams) {
    return issueDto.getType() != RuleType.SECURITY_HOTSPOT_VALUE &&
      (!queryParams.isResolvedOnly() || issueDto.getStatus().equals("RESOLVED"));
  }

  private void validateRuleRepositories(List<String> ruleRepositories) {
    checkArgument(ruleRepositories
      .stream()
      .noneMatch(taintChecker.getTaintRepositories()::contains),
      "Incorrect rule repositories list: it should only include repositories that define Issues, and no Taint Vulnerabilities");
  }
}
