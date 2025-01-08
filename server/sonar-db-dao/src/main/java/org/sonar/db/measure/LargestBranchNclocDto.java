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
package org.sonar.db.measure;

public class LargestBranchNclocDto {

  private String projectUuid;
  private String projectName;
  private String projectKey;
  private long loc;
  private String branchName;
  private String branchType;

  public String getProjectUuid() {
    return projectUuid;
  }

  public LargestBranchNclocDto setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  public String getProjectName() {
    return projectName;
  }

  public LargestBranchNclocDto setProjectName(String projectName) {
    this.projectName = projectName;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public LargestBranchNclocDto setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public String getBranchName() {
    return branchName;
  }

  public LargestBranchNclocDto setBranchName(String branchName) {
    this.branchName = branchName;
    return this;
  }

  public String getBranchType() {
    return branchType;
  }

  public LargestBranchNclocDto setBranchType(String branchType) {
    this.branchType = branchType;
    return this;
  }

  public long getLoc() {
    return loc;
  }

  public LargestBranchNclocDto setLoc(long loc) {
    this.loc = loc;
    return this;
  }
}
