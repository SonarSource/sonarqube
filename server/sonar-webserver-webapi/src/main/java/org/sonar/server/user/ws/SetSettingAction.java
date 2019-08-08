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
package org.sonar.server.user.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserPropertyDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserUpdater;

import static java.util.Objects.requireNonNull;

public class SetSettingAction implements UsersWsAction {

  public static final String PARAM_KEY = "key";
  public static final String PARAM_VALUE = "value";

  private final DbClient dbClient;
  private final UserSession userSession;

  public SetSettingAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("set_setting")
      .setDescription("Update a setting value.<br>" +
        "Requires user to be authenticated")
      .setSince("7.6")
      .setInternal(true)
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(true)
      .setMaximumLength(100)
      .setDescription("Setting key")
      .setPossibleValues("notifications.optOut", UserUpdater.NOTIFICATIONS_READ_DATE, "newsbox.dismiss.hotspots");

    action.createParam(PARAM_VALUE)
      .setRequired(true)
      .setMaximumLength(4000)
      .setDescription("Setting value")
      .setExampleValue("true");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    String key = request.mandatoryParam(PARAM_KEY);
    String value = request.mandatoryParam(PARAM_VALUE);
    setUserSetting(key, value);
    response.noContent();
  }

  private void setUserSetting(String key, String value) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.userPropertiesDao().insertOrUpdate(dbSession,
        new UserPropertyDto()
          .setUserUuid(requireNonNull(userSession.getUuid(), "Authenticated user uuid cannot be null"))
          .setKey(key)
          .setValue(value));
      dbSession.commit();
    }
  }

}
