/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.root.ws;

import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.user.UserQuery;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Roots;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements RootsWsAction {
  private final UserSession userSession;
  private final DbClient dbClient;

  public SearchAction(UserSession userSession, DbClient dbClient) {
    this.userSession = userSession;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("search")
        .setInternal(true)
        .setPost(false)
        .setDescription("Search for root users.<br/>" +
            "Requires to be root.")
        .setSince("6.2")
        .setResponseExample(getClass().getResource("search-example.json"))
        .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsRoot();

    try (DbSession dbSession = dbClient.openSession(false)) {
      List<UserDto> userDtos = dbClient.userDao().selectUsers(
          dbSession,
          UserQuery.builder()
              .mustBeRoot()
              .build());

      writeResponse(request, response, userDtos);
    }
  }

  private static void writeResponse(Request request, Response response, List<UserDto> dtos) {
    Roots.SearchResponse.Builder responseBuilder = Roots.SearchResponse.newBuilder();
    Roots.RootContent.Builder rootBuilder = Roots.RootContent.newBuilder();
    dtos.forEach(dto -> responseBuilder.addRoots(toRoot(rootBuilder, dto)));
    writeProtobuf(responseBuilder.build(), request, response);
  }

  private static Roots.RootContent toRoot(Roots.RootContent.Builder builder, UserDto dto) {
    builder.clear();
    builder.setLogin(dto.getLogin());
    if (dto.getName() != null) {
      builder.setName(dto.getName());
    }
    if (dto.getEmail() != null) {
      builder.setEmail(dto.getEmail());
    }
    return builder.build();
  }

}
