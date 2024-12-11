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
package org.sonarqube.ws.client.almintegrations;

import jakarta.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/search_bitbucketserver_repos">Further information about this action online (including a response example)</a>
 * @since 8.2
 */
@Generated("sonar-ws-generator")
public class SearchBitbucketcloudReposRequest {

  private String almSetting;
  private String projectName;
  private String repositoryName;

  /**
   * This is a mandatory parameter.
   */
  public SearchBitbucketcloudReposRequest setAlmSetting(String almSetting) {
    this.almSetting = almSetting;
    return this;
  }

  public String getAlmSetting() {
    return almSetting;
  }

  /**
   */
  public SearchBitbucketcloudReposRequest setProjectName(String projectName) {
    this.projectName = projectName;
    return this;
  }

  public String getProjectName() {
    return projectName;
  }

  /**
   */
  public SearchBitbucketcloudReposRequest setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
    return this;
  }

  public String getRepositoryName() {
    return repositoryName;
  }
}
