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
package org.sonar.server.setting.ws;

import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Settings.CheckSecretKeyWsResponse;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CheckSecretKeyAction implements SettingsWsAction {
  private final Settings settings;
  private final UserSession userSession;

  public CheckSecretKeyAction(Settings settings, UserSession userSession) {
    this.settings = settings;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("check_secret_key")
      .setDescription("Check if a secret key is available.<br>" +
        "Requires the 'Administer System' permission.")
      .setSince("6.1")
      .setInternal(true)
      .setResponseExample(getClass().getResource("check_secret_key-example.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    writeProtobuf(CheckSecretKeyWsResponse.newBuilder().setSecretKeyAvailable(settings.getEncryption().hasSecretKey()).build(), request, response);
  }
}
