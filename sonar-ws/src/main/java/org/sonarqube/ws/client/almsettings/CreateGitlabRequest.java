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
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_settings/create_gitlab">Further information about this action online (including a response example)</a>
 * @since 8.1
 */
@Generated("sonar-ws-generator")
public class CreateGitlabRequest {

  private String key;
  private String personalAccessToken;
  private String url;

  public String getUrl() {
    return url;
  }

  public CreateGitlabRequest setUrl(String url) {
    this.url = url;
    return this;
  }

  /**
   * This is a mandatory parameter.
   */
  public CreateGitlabRequest setKey(String key) {
    this.key = key;
    return this;
  }

  public String getKey() {
    return key;
  }

  /**
   * This is a mandatory parameter.
   */
  public CreateGitlabRequest setPersonalAccessToken(String personalAccessToken) {
    this.personalAccessToken = personalAccessToken;
    return this;
  }

  public String getPersonalAccessToken() {
    return personalAccessToken;
  }

}
