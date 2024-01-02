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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.issue.IssueQueryParams;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.TaintChecker;
import org.sonar.server.issue.ws.pull.PullTaintActionProtobufObjectGenerator;
import org.sonar.server.user.UserSession;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_PULL_TAINT;

public class PullTaintAction extends BasePullAction {
  private static final String ISSUE_TYPE = "taint vulnerabilities";
  private static final String RESOURCE_EXAMPLE = "pull-taint-example.proto";
  private static final String SINCE_VERSION = "9.6";

  private final DbClient dbClient;
  private final TaintChecker taintChecker;

  public PullTaintAction(System2 system2, ComponentFinder componentFinder, DbClient dbClient, UserSession userSession,
    PullTaintActionProtobufObjectGenerator protobufObjectGenerator, TaintChecker taintChecker) {
    super(system2, componentFinder, dbClient, userSession, protobufObjectGenerator, ACTION_PULL_TAINT,
      ISSUE_TYPE, "", SINCE_VERSION, RESOURCE_EXAMPLE);
    this.dbClient = dbClient;
    this.taintChecker = taintChecker;
  }

  @Override
  protected Set<String> getIssueKeysSnapshot(IssueQueryParams issueQueryParams, int page) {
    Optional<Long> changedSinceDate = ofNullable(issueQueryParams.getChangedSince());

    try (DbSession dbSession = dbClient.openSession(false)) {
      if (changedSinceDate.isPresent()) {
        return dbClient.issueDao().selectIssueKeysByComponentUuidAndChangedSinceDate(dbSession, issueQueryParams.getBranchUuid(),
          changedSinceDate.get(), issueQueryParams.getRuleRepositories(), emptyList(),
          issueQueryParams.getLanguages(), page);
      }

      return dbClient.issueDao().selectIssueKeysByComponentUuid(dbSession, issueQueryParams.getBranchUuid(),
        issueQueryParams.getRuleRepositories(),
        emptyList(), issueQueryParams.getLanguages(),page);

    }
  }

  @Override
  protected IssueQueryParams initializeQueryParams(BranchDto branchDto, @Nullable List<String> languages,
    @Nullable List<String> ruleRepositories, boolean resolvedOnly, @Nullable Long changedSince) {
    return new IssueQueryParams(branchDto.getUuid(), languages, taintChecker.getTaintRepositories(), emptyList(), false, changedSince);
  }

  @Override
  protected void validateRuleRepositories(List<String> ruleRepositories) {
  }
}
