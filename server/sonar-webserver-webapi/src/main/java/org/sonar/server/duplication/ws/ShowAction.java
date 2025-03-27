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
package org.sonar.server.duplication.ws;

import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ShowAction implements DuplicationsWsAction {

  private static final String PARAM_KEY = "key";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_PULL_REQUEST = "pullRequest";
  private final DbClient dbClient;
  private final DuplicationsParser parser;
  private final ShowResponseBuilder responseBuilder;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public ShowAction(DbClient dbClient, DuplicationsParser parser, ShowResponseBuilder responseBuilder, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.parser = parser;
    this.responseBuilder = responseBuilder;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("show")
      .setDescription("Get duplications. Require Browse permission on file's project")
      .setSince("4.4")
      .setHandler(this)
      .setResponseExample(getClass().getResource("show-example.json"));

    action.setChangelog(
      new Change("9.6", "The fields 'subProject', 'subProjectName' were removed from the response."),
      new Change("8.8", "Deprecated parameter 'uuid' was removed."),
      new Change("8.8", "The fields 'uuid', 'projectUuid', 'subProjectUuid' were removed from the response."),
      new Change("6.5", "Parameter 'uuid' is now deprecated."),
      new Change("6.5", "The fields 'uuid', 'projectUuid', 'subProjectUuid' are now deprecated in the response."));

    action
      .createParam(PARAM_KEY)
      .setDescription("File key")
      .setRequired(true)
      .setExampleValue("my_project:/src/foo/Bar.php");

    action
      .createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setInternal(true)
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);

    action
      .createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id")
      .setInternal(true)
      .setSince("7.1")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = loadComponent(dbSession, request);
      BranchDto branchDto = loadBranch(dbSession, component);
      String branch = branchDto.isMain() ? null : branchDto.getBranchKey();
      String pullRequest = branchDto.getPullRequestKey();
      userSession.checkComponentPermission(ProjectPermission.CODEVIEWER, component);
      String duplications = findDataFromComponent(dbSession, component);
      List<DuplicationsParser.Block> blocks = parser.parse(dbSession, component, branch, pullRequest, duplications);
      writeProtobuf(responseBuilder.build(dbSession, blocks, branch, pullRequest), request, response);
    }
  }

  private BranchDto loadBranch(DbSession dbSession, ComponentDto component) {
    Optional<BranchDto> branchDto = dbClient.branchDao().selectByUuid(dbSession, component.branchUuid());
    if (branchDto.isEmpty()) {
      throw new IllegalStateException("Could not find a branch for component with " + component.uuid());
    }
    return branchDto.get();
  }

  private ComponentDto loadComponent(DbSession dbSession, Request request) {
    String key = request.mandatoryParam(PARAM_KEY);
    String branch = request.param(PARAM_BRANCH);
    String pullRequest = request.param(PARAM_PULL_REQUEST);
    if (branch == null && pullRequest == null) {
      return componentFinder.getByKey(dbSession, key);
    }
    return componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, key, branch, pullRequest);
  }

  @CheckForNull
  private String findDataFromComponent(DbSession dbSession, ComponentDto component) {
    return dbClient.measureDao().selectByComponentUuid(dbSession, component.uuid())
      .map(m -> m.getString(CoreMetrics.DUPLICATIONS_DATA_KEY))
      .orElse(null);
  }
}
