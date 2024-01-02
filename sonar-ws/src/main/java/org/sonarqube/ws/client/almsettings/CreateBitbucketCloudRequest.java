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

import javax.annotation.Generated;

/**
 *
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_settings/create_bitbucket_cloud">Further information about this action online (including a response example)</a>
 * @since 8.7
 */
@Generated("sonar-ws-generator")
public class CreateBitbucketCloudRequest {

  private String key;
  private String clientId;
  private String clientSecret;
  private String workspace;

  public String getKey() {
    return key;
  }

  /**
   * This is a mandatory parameter.
   */
  public CreateBitbucketCloudRequest setKey(String key) {
    this.key = key;
    return this;
  }

  public String getClientId() {
    return clientId;
  }

  /**
   * This is a mandatory parameter.
   */
  public CreateBitbucketCloudRequest setClientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  /**
   * This is a mandatory parameter.
   */
  public CreateBitbucketCloudRequest setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

  public String getWorkspace() {
    return workspace;
  }

  /**
   * This is a mandatory parameter.
   */
  public CreateBitbucketCloudRequest setWorkspace(String workspace) {
    this.workspace = workspace;
    return this;
  }
}
