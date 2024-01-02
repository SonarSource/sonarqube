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
package org.sonar.server.projectanalysis.ws;

import com.google.protobuf.Empty;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_BRANCH;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_PROJECT;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class UnsetBaselineAction implements ProjectAnalysesWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final BranchDao branchDao;

  public UnsetBaselineAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder, BranchDao branchDao) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.branchDao = branchDao;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("unset_baseline")
      .setDescription("Unset any manually-set New Code Period baseline on a project or a branch.<br/>" +
        "Unsetting a manual baseline restores the use of the default new code period setting.<br/>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer System'</li>" +
        "  <li>'Administer' rights on the specified project</li>" +
        "</ul>")
      .setSince("7.7")
      .setPost(true)
      .setDeprecatedSince("8.0")
      .setHandler(this);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(true);

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key");
  }

  @Override
  public void handle(Request httpRequest, Response httpResponse) throws Exception {
    doHandle(httpRequest);

    writeProtobuf(Empty.newBuilder().build(), httpRequest, httpResponse);
  }

  private void doHandle(Request request) {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    String branchKey = trimToNull(request.param(PARAM_BRANCH));

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
      userSession.checkProjectPermission(UserRole.ADMIN, project);

      BranchDto branch = loadBranch(dbSession, project, branchKey);
      dbClient.newCodePeriodDao().delete(dbSession, project.getUuid(), branch.getUuid());
      dbSession.commit();
    }
  }

  private BranchDto loadBranch(DbSession dbSession, ProjectDto project, @Nullable String branchKey) {
    if (branchKey != null) {
      return branchDao.selectByBranchKey(dbSession, project.getUuid(), branchKey)
        .orElseThrow(() -> new NotFoundException(String.format("Branch '%s' in project '%s' not found", branchKey, project.getKey())));
    }

    return branchDao.selectByUuid(dbSession, project.getUuid())
      .orElseThrow(() -> new NotFoundException(String.format("Main branch in project '%s' not found", project.getKey())));
  }

}
