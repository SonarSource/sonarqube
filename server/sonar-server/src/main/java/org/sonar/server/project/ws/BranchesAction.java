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
package org.sonar.server.project.ws;

import java.util.Collection;
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
import org.sonarqube.ws.WsProjects;

import static org.sonar.core.util.Protobuf.setNullable;

public class BranchesAction implements ProjectsWsAction {

  private static final String PROJECT_PARAM = "project";

  private final DbClient dbClient;
  private final UserSession userSession;

  public BranchesAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("branches")
      .setSince("6.6")
      .setHandler(this);

    action
      .createParam(PROJECT_PARAM)
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PROJECT_PARAM);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = dbClient.componentDao().selectOrFailByKey(dbSession, projectKey);
      userSession.checkComponentPermission(UserRole.USER, project);
      Collection<BranchDto> branches = dbClient.branchDao().selectByComponent(dbSession, project);

      WsProjects.BranchesWsResponse.Builder protobufResponse = WsProjects.BranchesWsResponse.newBuilder();
      branches.stream()
        .filter(b -> b.getKeeType().equals(BranchKeyType.BRANCH))
        .forEach(b -> addToProtobuf(protobufResponse, b));
      WsUtils.writeProtobuf(protobufResponse.build(), request, response);
    }
  }

  private static void addToProtobuf(WsProjects.BranchesWsResponse.Builder response, BranchDto branch) {
    WsProjects.BranchesWsResponse.Branch.Builder builder = response.addBranchesBuilder();
    setNullable(branch.getKey(), builder::setName);
    builder.setIsMain(branch.isMain());
    builder.setType(WsProjects.BranchesWsResponse.BranchType.valueOf(branch.getBranchType().name()));
    builder.build();
  }
}
