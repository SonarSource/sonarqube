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
package org.sonar.server.hotspot.ws;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueQueryParams;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.ws.BasePullAction;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;

import static java.util.Collections.emptyList;

public class PullAction extends BasePullAction implements HotspotsWsAction {
  private static final String ISSUE_TYPE = "hotspots";
  private static final String ACTION_NAME = "pull";
  private static final String RESOURCE_EXAMPLE = "pull-hotspot-example.proto";
  private static final String SINCE_VERSION = "10.1";

  private final DbClient dbClient;

  public PullAction(System2 system2, ComponentFinder componentFinder, DbClient dbClient, UserSession userSession,
    PullHotspotsActionProtobufObjectGenerator protobufObjectGenerator) {
    super(system2, componentFinder, dbClient, userSession, protobufObjectGenerator, ACTION_NAME,
      ISSUE_TYPE, "", SINCE_VERSION, RESOURCE_EXAMPLE);
    this.dbClient = dbClient;
  }

  @Override
  protected Set<String> getIssueKeysSnapshot(IssueQueryParams issueQueryParams, int page) {
    Long changedSinceDate = issueQueryParams.getChangedSince();

    try (DbSession dbSession = dbClient.openSession(false)) {
      if (changedSinceDate != null) {
        return dbClient.issueDao().selectIssueKeysByComponentUuidAndChangedSinceDate(dbSession, issueQueryParams.getBranchUuid(),
          changedSinceDate, issueQueryParams.getRuleRepositories(), emptyList(),
          issueQueryParams.getLanguages(), page);
      }

      return dbClient.issueDao().selectIssueKeysByComponentUuid(dbSession, issueQueryParams.getBranchUuid(),
        issueQueryParams.getRuleRepositories(),
        emptyList(), issueQueryParams.getLanguages(), page);

    }
  }

  @Override
  protected IssueQueryParams initializeQueryParams(BranchDto branchDto, @Nullable List<String> languages,
    @Nullable List<String> ruleRepositories, boolean resolvedOnly, @Nullable Long changedSince) {
    return new IssueQueryParams(branchDto.getUuid(), languages, emptyList(), emptyList(), false, changedSince);
  }

  @Override
  protected boolean filterNonClosedIssues(IssueDto issueDto, IssueQueryParams queryParams) {
    return issueDto.getType() == Common.RuleType.SECURITY_HOTSPOT_VALUE;
  }

}
