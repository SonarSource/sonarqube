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
package org.sonarqube.ws.client.almsettings;

import javax.annotation.Generated;

/**
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_settings/update_gitlab">Further information about this action online (including a response example)</a>
 * @since 8.1
 */
@Generated("sonar-ws-generator")
public class UpdateGitlabRequest {

  private String key;
  private String newKey;
  private String personalAccessToken;

  public String getKey() {
    return key;
  }

  /**
   * This is a mandatory parameter.
   */
  public UpdateGitlabRequest setKey(String key) {
    this.key = key;
    return this;
  }

  public String getNewKey() {
    return newKey;
  }

  /**
   */
  public UpdateGitlabRequest setNewKey(String newKey) {
    this.newKey = newKey;
    return this;
  }

  public String getPersonalAccessToken() {
    return personalAccessToken;
  }

  /**
   * This is a mandatory parameter.
   */
  public UpdateGitlabRequest setPersonalAccessToken(String personalAccessToken) {
    this.personalAccessToken = personalAccessToken;
    return this;
  }

}
