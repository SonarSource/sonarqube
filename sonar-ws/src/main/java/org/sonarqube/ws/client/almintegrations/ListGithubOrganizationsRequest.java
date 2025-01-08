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
package org.sonarqube.ws.client.almintegrations;

/**
 * This is part of the internal API.
 * This is a GET request.
 *
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/list_github_organizations">Further information about this action online (including a response example)</a>
 */
public class ListGithubOrganizationsRequest {

  private String almSetting;
  private String token;



  /**
   * This is a mandatory parameter.
   */
  public ListGithubOrganizationsRequest setAlmSetting(String almSetting) {
    this.almSetting = almSetting;
    return this;
  }

  public String getAlmSetting() {
    return almSetting;
  }

  public String getToken() {
    return token;
  }

  public ListGithubOrganizationsRequest setToken(String token) {
    this.token = token;
    return this;
  }
}
