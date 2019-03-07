/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.sonar.server.component.ComponentFinder.ParamNames.PROJECT_ID_AND_KEY;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_ANALYSIS;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_BRANCH;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_PROJECT;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SetBaselineAction implements ProjectAnalysesWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public SetBaselineAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("set_baseline")
      .setDescription("Set an analysis as the baseline of the New Code Period on a project or a long-lived branch.<br/>" +
        "This manually set baseline overrides the `sonar.leak.period` setting.<br/>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer System'</li>" +
        "  <li>'Administer' rights on the specified project</li>" +
        "</ul>")
      .setSince("7.7")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(true);

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key");

    action.createParam(PARAM_ANALYSIS)
      .setDescription("Analysis key")
      .setExampleValue(Uuids.UUID_EXAMPLE_01)
      .setRequired(true);
  }

  @Override
  public void handle(Request httpRequest, Response httpResponse) throws Exception {
    doHandle(httpRequest);

    writeProtobuf(Empty.newBuilder().build(), httpRequest, httpResponse);
  }

  private void doHandle(Request request) {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    String branchKey = trimToNull(request.param(PARAM_BRANCH));
    String analysisUuid = request.mandatoryParam(PARAM_ANALYSIS);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto projectBranch = getProjectBranch(dbSession, projectKey, branchKey);
      SnapshotDto analysis = getAnalysis(dbSession, analysisUuid);
      checkRequest(dbSession, projectBranch, branchKey, analysis);
      dbClient.branchDao().updateManualBaseline(dbSession, projectBranch.uuid(), analysis.getUuid());
      dbSession.commit();
    }
  }

  private ComponentDto getProjectBranch(DbSession dbSession, String projectKey, @Nullable String branchKey) {
    if (branchKey == null) {
      return componentFinder.getByUuidOrKey(dbSession, null, projectKey, PROJECT_ID_AND_KEY);
    }
    ComponentDto project = componentFinder.getByKeyAndBranch(dbSession, projectKey, branchKey);

    BranchDto branchDto = dbClient.branchDao().selectByUuid(dbSession, project.uuid())
      .orElseThrow(() -> new NotFoundException(format("Branch '%s' is not found", branchKey)));

    checkArgument(branchDto.getBranchType() == BranchType.LONG,
      "Not a long-living branch: '%s'", branchKey);

    return project;
  }

  private SnapshotDto getAnalysis(DbSession dbSession, String analysisUuid) {
    return dbClient.snapshotDao().selectByUuid(dbSession, analysisUuid)
      .orElseThrow(() -> new NotFoundException(format("Analysis '%s' is not found", analysisUuid)));
  }

  private void checkRequest(DbSession dbSession, ComponentDto projectBranch, @Nullable String branchKey, SnapshotDto analysis) {
    userSession.checkComponentPermission(UserRole.ADMIN, projectBranch);
    ComponentDto project = dbClient.componentDao().selectByUuid(dbSession, analysis.getComponentUuid()).orElse(null);

    boolean analysisMatchesProjectBranch = project != null && projectBranch.uuid().equals(project.uuid());
    if (branchKey != null) {
      checkArgument (analysisMatchesProjectBranch,
        "Analysis '%s' does not belong to branch '%s' of project '%s'",
        analysis.getUuid(), branchKey, projectBranch.getKey());
    } else {
      checkArgument (analysisMatchesProjectBranch,
        "Analysis '%s' does not belong to project '%s'",
        analysis.getUuid(), projectBranch.getKey());
    }
  }
}
