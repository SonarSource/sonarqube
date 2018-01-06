/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/backup">Further information about this action online (including a response example)</a>
 * @since 5.2
 */
@Generated("sonar-ws-generator")
public class BackupRequest {

  private String key;
  private String language;
  private String organization;
  private String qualityProfile;

  /**
   * Example value: "AU-Tpxb--iU5OvuD2FLy"
   * @deprecated since 6.6
   */
  @Deprecated
  public BackupRequest setKey(String key) {
    this.key = key;
    return this;
  }

  public String getKey() {
    return key;
  }

  /**
   */
  public BackupRequest setLanguage(String language) {
    this.language = language;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  /**
   * This is part of the internal API.
   * Example value: "my-org"
   */
  public BackupRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * Example value: "Sonar way"
   */
  public BackupRequest setQualityProfile(String qualityProfile) {
    this.qualityProfile = qualityProfile;
    return this;
  }

  public String getQualityProfile() {
    return qualityProfile;
  }
}
