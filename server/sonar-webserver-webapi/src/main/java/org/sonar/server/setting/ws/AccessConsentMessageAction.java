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

import static org.sonar.core.config.WebConstants.CODESCAN_ACCESS_CONSENT_MESSAGE;
import static org.sonar.core.config.WebConstants.CODESCAN_ACCESS_DISPLAY_CONSENT_MESSAGE;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.property.PropertyDto;
import org.sonar.markdown.Markdown;

public class AccessConsentMessageAction implements SettingsWsAction {
  private final DbClient dbClient;

  public AccessConsentMessageAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("access_consent_message")
      .setDescription("Returns data access consent message" + CODESCAN_ACCESS_CONSENT_MESSAGE + "' property.")
      .setSince("10.8")
      .setInternal(true)
      .setResponseExample(Resources.getResource(getClass(), "example-access-consent-message.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (JsonWriter writer = response.newJsonWriter()) {
      writer.beginObject()
        .prop("message", isMessageDisplayEnabled() ? Markdown.convertToHtml(getAccessConsentMessage()) : "")
        .endObject()
        .close();
    }
  }

  /**
   * Gets the boolean value of the property "codescan.cloud.accessConsent.displayMessage".
   * @return True if the property is enabled, false if it's disabled or not set.
   */
  private boolean isMessageDisplayEnabled() {
    PropertyDto displayMessageProperty = dbClient.propertiesDao().selectGlobalProperty(CODESCAN_ACCESS_DISPLAY_CONSENT_MESSAGE);
    return displayMessageProperty != null && Boolean.TRUE.toString().equals(displayMessageProperty.getValue());
  }

  /**
   * Retrieves the String value of the property "codescan.cloud.accessConsent.message".
   * @return The saved String value, or empty String if it's not set.
   */
  private String getAccessConsentMessage() {
    PropertyDto accessConsentMessageProperty = dbClient.propertiesDao().selectGlobalProperty(CODESCAN_ACCESS_CONSENT_MESSAGE);
    return accessConsentMessageProperty != null && accessConsentMessageProperty.getValue() != null ? accessConsentMessageProperty.getValue() : "";
  }
}