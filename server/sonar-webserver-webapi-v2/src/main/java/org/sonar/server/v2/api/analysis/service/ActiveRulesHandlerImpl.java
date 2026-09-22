/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.api.analysis.service;

import java.util.List;
import java.util.Optional;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.rule.ActiveRuleRestReponse;
import org.sonar.server.rule.ActiveRuleService;
import org.sonar.server.user.UserSession;

import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

public class ActiveRulesHandlerImpl implements ActiveRulesHandler {

  private final DbClient dbClient;
  private final ActiveRuleService activeRuleService;
  private final UserSession userSession;

  public ActiveRulesHandlerImpl(DbClient dbClient, ActiveRuleService activeRuleService, UserSession userSession) {
    this.dbClient = dbClient;
    this.activeRuleService = activeRuleService;
    this.userSession = userSession;
  }

  @Override
  public List<ActiveRuleRestReponse.ActiveRule> getActiveRules(String projectKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      boolean hasGlobalScanPermission = userSession.hasPermission(GlobalPermission.SCAN);
      Optional<ProjectDto> project = dbClient.projectDao().selectProjectByKey(dbSession, projectKey);

      if (project.isPresent()) {
        if (hasGlobalScanPermission || userSession.hasEntityPermission(ProjectPermission.SCAN, project.get())) {
          return activeRuleService.buildActiveRules(project.get().getUuid());
        }
      } else if (hasGlobalScanPermission || userSession.hasPermission(GlobalPermission.PROVISION_PROJECTS)) {
        // ReportSubmitter.createProject requires PROVISION_PROJECTS to create a project on first analysis,
        // independently of how scan permission on it is later granted (global, default template, or DevOps platform).
        return activeRuleService.buildDefaultActiveRules();
      }

      throw insufficientPrivilegesException();
    }
  }

}
