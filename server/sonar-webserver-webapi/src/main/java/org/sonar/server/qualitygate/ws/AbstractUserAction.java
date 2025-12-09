/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.UserDto;

import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.qualitygate.ws.CreateAction.NAME_MAXIMUM_LENGTH;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;

public abstract class AbstractUserAction implements QualityGatesWsAction {
  protected final DbClient dbClient;
  protected final QualityGatesWsSupport wsSupport;

  protected AbstractUserAction(DbClient dbClient, QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
  }

  protected void defineGateAndUserParameters(WebService.NewAction action) {
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
      apply(dbSession, qualityGateDto, user);
    }
    response.noContent();
  }

  private UserDto getUser(DbSession dbSession, String login) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
    checkFound(user, "User with login '%s' is not found", login);
    return user;
  }

  protected abstract void apply(DbSession dbSession, QualityGateDto qualityGate, UserDto user);
}
