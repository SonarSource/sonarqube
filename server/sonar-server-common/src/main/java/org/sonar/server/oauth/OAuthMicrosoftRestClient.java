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
package org.sonar.server.oauth;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class OAuthMicrosoftRestClient {

  public String getAccessTokenFromClientCredentialsGrantFlow(String host, String clientId, String clientSecret, String tenant, String scope) {
    final OAuth20Service service = new ServiceBuilder(clientId)
      .apiSecret(clientSecret)
      .defaultScope(scope)
      .build(new ScribeMicrosoftOauth2Api(host, tenant));
    try {
      return service.getAccessTokenClientCredentialsGrant().getAccessToken();
    } catch (IOException | ExecutionException e) {
      throw new IllegalStateException("Unable to get a token: " + e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while getting a token: " + e);
    }
  }
}
