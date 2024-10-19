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
package org.sonar.server.qualitygate.ws;

import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.UserDto;

import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_REMOVE_USER;

public class RemoveUserAction extends AbstractUserAction {

  public RemoveUserAction(DbClient dbClient, QualityGatesWsSupport wsSupport) {
    super(dbClient, wsSupport);
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction(ACTION_REMOVE_USER)
      .setDescription("Remove the ability from an user to edit a Quality Gate.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Gates'</li>" +
        "  <li>Edit right on the specified quality gate</li>" +
        "</ul>")
      .setHandler(this)
      .setPost(true)
      .setSince("9.2");

    super.defineGateAndUserParameters(action);
  }

  @Override
  protected void apply(DbSession dbSession, QualityGateDto qualityGate, UserDto user) {
    if (!dbClient.qualityGateUserPermissionDao().exists(dbSession, qualityGate, user)) {
      return;
    }
    dbClient.qualityGateUserPermissionDao().deleteByQualityGateAndUser(dbSession, qualityGate, user);
    dbSession.commit();
  }
}
