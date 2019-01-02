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
package org.sonarqube.ws.client.qualityprofiles;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/remove_user">Further information about this action online (including a response example)</a>
 * @since 6.6
 */
@Generated("sonar-ws-generator")
public class RemoveUserRequest {

  private String language;
  private String login;
  private String organization;
  private String qualityProfile;

  /**
   * This is a mandatory parameter.
   */
  public RemoveUserRequest setLanguage(String language) {
    this.language = language;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "john.doe"
   */
  public RemoveUserRequest setLogin(String login) {
    this.login = login;
    return this;
  }

  public String getLogin() {
    return login;
  }

  /**
   * This is part of the internal API.
   * Example value: "my-org"
   */
  public RemoveUserRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "Recommended quality profile"
   */
  public RemoveUserRequest setQualityProfile(String qualityProfile) {
    this.qualityProfile = qualityProfile;
    return this;
  }

  public String getQualityProfile() {
    return qualityProfile;
  }
}
