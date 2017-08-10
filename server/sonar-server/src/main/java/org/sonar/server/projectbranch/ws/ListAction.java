/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.projectbranch.ws;

import com.google.common.io.Resources;
import java.util.Collection;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchKeyType;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.WsBranches;

import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonarqube.ws.client.projectbranches.ProjectBranchesParameters.ACTION_LIST;
import static org.sonarqube.ws.client.projectbranches.ProjectBranchesParameters.PARAM_PROJECT;

public class ListAction implements BranchWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;

  public ListAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_LIST)
      .setSince("6.6")
      .setDescription("List the branches of a project")
      .setResponseExample(Resources.getResource(getClass(), "list-example.json"))
      .setHandler(this);

    action
      .createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = WsUtils.checkFoundWithOptional(
        dbClient.componentDao().selectByKey(dbSession, projectKey),
        "Project key '%s' not found", projectKey);

      userSession.checkComponentPermission(UserRole.USER, project);
      if (!project.isEnabled() || !Qualifiers.PROJECT.equals(project.qualifier())) {
        throw new IllegalArgumentException("Invalid project key");
      }

      Collection<BranchDto> branches = dbClient.branchDao().selectByComponent(dbSession, project);
      WsBranches.ListWsResponse.Builder protobufResponse = WsBranches.ListWsResponse.newBuilder();
      branches.stream()
        .filter(b -> b.getKeeType().equals(BranchKeyType.BRANCH))
        .forEach(b -> addToProtobuf(protobufResponse, b));
      WsUtils.writeProtobuf(protobufResponse.build(), request, response);
    }
  }

  private static void addToProtobuf(WsBranches.ListWsResponse.Builder response, BranchDto branch) {
    WsBranches.ListWsResponse.Branch.Builder builder = response.addBranchesBuilder();
    setNullable(branch.getKey(), builder::setName);
    builder.setIsMain(branch.isMain());
    builder.setType(WsBranches.ListWsResponse.BranchType.valueOf(branch.getBranchType().name()));
    builder.build();
  }
}
