/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.server.qualitygate.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualitygate.QualityGateUserPermissionsDto;
import org.sonar.db.user.UserDto;

import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.qualitygate.ws.CreateAction.NAME_MAXIMUM_LENGTH;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_ADD_USER;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;

public class AddUserAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final QualityGatesWsSupport wsSupport;

  public AddUserAction(DbClient dbClient, UuidFactory uuidFactory, QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction(ACTION_ADD_USER)
      .setDescription("Allow a user to edit a Quality Gate.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Gates'</li>" +
        "  <li>Edit right on the specified quality gate</li>" +
        "</ul>")
      .setHandler(this)
      .setPost(true)
      .setInternal(true)
      .setSince("9.2");

    action.createParam(PARAM_GATE_NAME)
      .setDescription("Quality Gate name")
      .setRequired(true)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setExampleValue("SonarSource Way");

    action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("john.doe");

  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    final String login = request.mandatoryParam(PARAM_LOGIN);
    final String qualityGateName = request.mandatoryParam(PARAM_GATE_NAME);

    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityGateDto qualityGateDto = wsSupport.getByName(dbSession, qualityGateName);
      wsSupport.checkCanLimitedEdit(dbSession, qualityGateDto);
      UserDto user = getUser(dbSession, login);
      addUser(dbSession, qualityGateDto, user);
    }
    response.noContent();
  }

  private UserDto getUser(DbSession dbSession, String login) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
    checkFound(user, "User with login '%s' is not found", login);
    return user;
  }

  private void addUser(DbSession dbSession, QualityGateDto qualityGate, UserDto user) {
    if (dbClient.qualityGateUserPermissionDao().exists(dbSession, qualityGate, user)) {
      return;
    }
    dbClient.qualityGateUserPermissionDao().insert(dbSession,
      new QualityGateUserPermissionsDto()
        .setUuid(uuidFactory.create())
        .setUserUuid(user.getUuid())
        .setQualityGateUuid(qualityGate.getUuid()));
    dbSession.commit();
  }

}
