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

import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.property.PropertyDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.loginmessage.LoginMessageFeature;

import static org.sonar.core.config.WebConstants.SONAR_LOGIN_DISPLAY_MESSAGE;
import static org.sonar.core.config.WebConstants.SONAR_LOGIN_MESSAGE;

public class LoginMessageAction implements SettingsWsAction {
  private final DbClient dbClient;
  private final LoginMessageFeature loginMessageFeature;

  public LoginMessageAction(DbClient dbClient, LoginMessageFeature loginMessageFeature) {
    this.dbClient = dbClient;
    this.loginMessageFeature = loginMessageFeature;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("login_message")
      .setDescription("Returns the formatted login message, set to the '" + SONAR_LOGIN_MESSAGE + "' property.")
      .setSince("9.8")
      .setInternal(true)
      .setResponseExample(Resources.getResource(getClass(), "example-login-message.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (JsonWriter writer = response.newJsonWriter()) {
      writer.beginObject()
        .prop("message", isMessageDisplayEnabled() && loginMessageFeature.isAvailable() ? Markdown.convertToHtml(getLoginMessage()) : "")
        .endObject()
        .close();
    }
  }

  /**
   * Gets the boolean value of the property "sonar.login.displayMessage".
   * @return True if the property is enabled, false if it's disabled or not set.
   */
  private boolean isMessageDisplayEnabled() {
    PropertyDto displayMessageProperty = dbClient.propertiesDao().selectGlobalProperty(SONAR_LOGIN_DISPLAY_MESSAGE);
    return displayMessageProperty != null && Boolean.TRUE.toString().equals(displayMessageProperty.getValue());
  }

  /**
   * Retrieves the String value of the property "sonar.login.message".
   * @return The saved String value, or empty String if it's not set.
   */
  private String getLoginMessage() {
    PropertyDto loginMessageProperty = dbClient.propertiesDao().selectGlobalProperty(SONAR_LOGIN_MESSAGE);
    return loginMessageProperty != null && loginMessageProperty.getValue() != null ? loginMessageProperty.getValue() : "";
  }
}
