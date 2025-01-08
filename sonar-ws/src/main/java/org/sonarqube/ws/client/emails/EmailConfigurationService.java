/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonarqube.ws.client.emails;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.DeleteRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.client.WsResponse;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

public class EmailConfigurationService extends BaseService {


  private static final String EMAIL_CONFIGURATION_ID = "email-configuration";

  public EmailConfigurationService(WsConnector wsConnector) {
    super(wsConnector, "api/v2/system/email-configurations");
  }

  public String createEmailConfiguration(WsEmailConfiguration config) {
    String body = String.format("""
        {
          "host": "%s",
          "port": "%s",
          "securityProtocol": "%s",
          "fromAddress": "%s",
          "fromName": "%s",
          "subjectPrefix": "%s",
          "authMethod": "%s",
          "username": "%s",
          "basicPassword": "%s",
          "oauthAuthenticationHost": "%s",
          "oauthClientId": "%s",
          "oauthClientSecret": "%s",
          "oauthTenant": "%s"
        }
        """,
      config.host(),
      config.port(),
      config.securityProtocol(),
      config.fromAddress(),
      config.fromName(),
      config.subjectPrefix(),
      config.authMethod(),
      config.username(),
      config.basicPassword(),
      config.oauthAuthenticationHost(),
      config.oauthClientId(),
      config.oauthClientSecret(),
      config.oauthTenant()
      );

    WsResponse response = call(
      new PostRequest(path()).setBody(body)
    );
    return new Gson().fromJson(response.content(), JsonObject.class).get("id").getAsString();
  }

  public void deleteEmailConfiguration() {
    try {
      call(new DeleteRequest(path() + "/" + EMAIL_CONFIGURATION_ID));
    } catch (HttpException e) {
      // We ignore if it gets deleted while there is no configuration
      if (e.code() != HTTP_NOT_FOUND) {
        throw e;
      }
    }
  }
}
