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
package org.sonar.server.hotspot.ws;

import java.util.Date;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder.ProjectAndBranch;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;

public class HotspotWsSupport {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final System2 system2;

  public HotspotWsSupport(DbClient dbClient, UserSession userSession, System2 system2) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.system2 = system2;
  }

  String checkLoggedIn() {
    return userSession.checkLoggedIn().getUuid();
  }

  ProjectAndBranch loadAndCheckBranch(DbSession dbSession, String hotspotKey) {
    IssueDto hotspot = loadHotspot(dbSession, hotspotKey);
    return loadAndCheckBranch(dbSession, hotspot, UserRole.USER);
  }

  IssueDto loadHotspot(DbSession dbSession, String hotspotKey) {
    return dbClient.issueDao().selectByKey(dbSession, hotspotKey)
      .filter(t -> t.getType() == RuleType.SECURITY_HOTSPOT.getDbConstant())
      .filter(t -> !Issue.STATUS_CLOSED.equals(t.getStatus()))
      .orElseThrow(() -> new NotFoundException(format("Hotspot '%s' does not exist", hotspotKey)));
  }

  ProjectAndBranch loadAndCheckBranch(DbSession dbSession, IssueDto hotspot, String userRole) {
    String branchUuid = hotspot.getProjectUuid();
    checkArgument(branchUuid != null, "Hotspot '%s' has no branch", hotspot.getKee());

    BranchDto branch = dbClient.branchDao().selectByUuid(dbSession, branchUuid)
      .orElseThrow(() -> new NotFoundException(format("Branch with uuid '%s' does not exist", branchUuid)));
    ProjectDto project = dbClient.projectDao().selectByUuid(dbSession, branch.getProjectUuid())
      .orElseThrow(() -> new NotFoundException(format("Project with uuid '%s' does not exist", branch.getProjectUuid())));

    userSession.checkEntityPermission(userRole, project);
    return new ProjectAndBranch(project, branch);
  }

  boolean canChangeStatus(ProjectDto project) {
    return userSession.hasEntityPermission(UserRole.SECURITYHOTSPOT_ADMIN, project);
  }

  IssueChangeContext newIssueChangeContextWithoutMeasureRefresh() {
    return issueChangeContextByUserBuilder(new Date(system2.now()), checkLoggedIn()).build();
  }

  IssueChangeContext newIssueChangeContextWithMeasureRefresh() {
    return issueChangeContextByUserBuilder(new Date(system2.now()), checkLoggedIn()).withRefreshMeasures().build();
  }
}
