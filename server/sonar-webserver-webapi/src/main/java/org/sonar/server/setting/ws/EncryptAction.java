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
package org.sonar.server.setting.ws;

import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Settings.EncryptWsResponse;

import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_VALUE;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class EncryptAction implements SettingsWsAction {
  private final UserSession userSession;
  private final Settings settings;

  public EncryptAction(UserSession userSession, Settings settings) {
    this.userSession = userSession;
    this.settings = settings;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("encrypt")
      .setDescription("Encrypt a setting value.<br>" +
        "Requires 'Administer System' permission.")
      .setSince("6.1")
      .setPost(true)
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("encrypt-example.json"))
      .setChangelog(new Change("10.4", "Move from GET to POST."));

    action.createParam(PARAM_VALUE)
      .setRequired(true)
      .setDescription("Setting value to encrypt")
      .setExampleValue("my value");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    String value = request.mandatoryParam(PARAM_VALUE);

    Encryption encryption = settings.getEncryption();
    checkRequest(encryption.hasSecretKey(), "No secret key available");

    String encryptedValue = encryption.encrypt(value);

    writeProtobuf(toEncryptWsResponse(encryptedValue), request, response);
  }

  private static EncryptWsResponse toEncryptWsResponse(String encryptedValue) {
    return EncryptWsResponse.newBuilder().setEncryptedValue(encryptedValue).build();
  }
}
