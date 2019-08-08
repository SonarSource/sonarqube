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
package org.sonar.server.batch;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ProjectDataQuery {

  private String projectKey;
  private String profileName;
  private String branch;
  private String pullRequest;

  private ProjectDataQuery() {
    // No direct call
  }

  @CheckForNull
  public String getProfileName() {
    return profileName;
  }

  public ProjectDataQuery setProfileName(@Nullable String profileName) {
    this.profileName = profileName;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public ProjectDataQuery setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  @CheckForNull
  public String getBranch() {
    return branch;
  }

  public ProjectDataQuery setBranch(@Nullable String branch) {
    this.branch = branch;
    return this;
  }

  @CheckForNull
  public String getPullRequest() {
    return pullRequest;
  }

  public ProjectDataQuery setPullRequest(@Nullable String pullRequest) {
    this.pullRequest = pullRequest;
    return this;
  }

  public static ProjectDataQuery create() {
    return new ProjectDataQuery();
  }
}
