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
package org.sonarqube.ws.client.almsettings;

import javax.annotation.CheckForNull;
import javax.annotation.Generated;
import javax.annotation.Nullable;

/**
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_settings/create_github">Further information about this action online (including a response example)</a>
 * @since 8.1
 */
@Generated("sonar-ws-generator")
public class CreateGithubRequest {

  private String appId;
  private String key;
  private String privateKey;
  private String url;
  private String clientId;
  private String clientSecret;
  private String webhookSecret;

  /**
   * This is a mandatory parameter.
   */
  public CreateGithubRequest setAppId(String appId) {
    this.appId = appId;
    return this;
  }

  public String getAppId() {
    return appId;
  }

  /**
   * This is a mandatory parameter.
   */
  public CreateGithubRequest setKey(String key) {
    this.key = key;
    return this;
  }

  public String getKey() {
    return key;
  }

  /**
   * This is a mandatory parameter.
   */
  public CreateGithubRequest setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
    return this;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  /**
   * This is a mandatory parameter.
   */
  public CreateGithubRequest setUrl(String url) {
    this.url = url;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public CreateGithubRequest setClientId(@Nullable String clientId) {
    this.clientId = clientId;
    return this;
  }

  @CheckForNull
  public String getClientId() {
    return clientId;
  }

  public CreateGithubRequest setClientSecret(@Nullable String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

  @CheckForNull
  public String getClientSecret() {
    return clientSecret;
  }

  public CreateGithubRequest setWebhookSecret(@Nullable String webhookSecret) {
    this.webhookSecret = webhookSecret;
    return this;
  }

  @CheckForNull
  public String getWebhookSecret() {
    return webhookSecret;
  }
}
