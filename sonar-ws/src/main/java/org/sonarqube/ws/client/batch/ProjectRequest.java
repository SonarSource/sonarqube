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
package org.sonarqube.ws.client.batch;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/batch/project">Further information about this action online (including a response example)</a>
 * @since 4.5
 */
@Generated("sonar-ws-generator")
public class ProjectRequest {

  private String branch;
  private String issuesMode;
  private String key;
  private String profile;

  /**
   * Example value: "feature/my_branch"
   */
  public ProjectRequest setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public String getBranch() {
    return branch;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public ProjectRequest setIssuesMode(String issuesMode) {
    this.issuesMode = issuesMode;
    return this;
  }

  public String getIssuesMode() {
    return issuesMode;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "my_project"
   */
  public ProjectRequest setKey(String key) {
    this.key = key;
    return this;
  }

  public String getKey() {
    return key;
  }

  /**
   * Example value: "SonarQube Way"
   */
  public ProjectRequest setProfile(String profile) {
    this.profile = profile;
    return this;
  }

  public String getProfile() {
    return profile;
  }
}
