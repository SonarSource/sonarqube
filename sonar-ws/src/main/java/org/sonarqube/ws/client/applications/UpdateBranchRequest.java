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
package org.sonarqube.ws.client.applications;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/applications/update_branch">Further information about this action online (including a response example)</a>
 * @since 7.3
 */
@Generated("sonar-ws-generator")
public class UpdateBranchRequest {

  private String application;
  private String branch;
  private String name;
  private List<String> projects;
  private List<String> projectBranches;

  /**
   * This is a mandatory parameter.
   * Example value: "my_application"
   */
  public UpdateBranchRequest setApplication(String application) {
    this.application = application;
    return this;
  }

  public String getApplication() {
    return application;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "branch-2.0"
   */
  public UpdateBranchRequest setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public String getBranch() {
    return branch;
  }

  /**
   * This is a mandatory parameter.
   */
  public UpdateBranchRequest setName(String name) {
    this.name = name;
    return this;
  }

  public String getName() {
    return name;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "project=firstProjectKey&project=secondProjectKey&project=thirdProjectKey"
   */
  public UpdateBranchRequest setProject(List<String> projects) {
    this.projects = projects;
    return this;
  }

  public List<String> getProject() {
    return projects;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "projectBranch=&projectBranch=branch-2.0&projectBranch=branch-2.1"
   */
  public UpdateBranchRequest setProjectBranch(List<String> projectBranches) {
    this.projectBranches = projectBranches;
    return this;
  }

  public List<String> getProjectBranch() {
    return projectBranches;
  }
}
