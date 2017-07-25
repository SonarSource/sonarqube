/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.db.component;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

public class BranchDto {

  /**
   * Value of {@link #kee} when the name of main branch is not known.
   * Used only if {@link #keeType} is {@link BranchKeyType#BRANCH}.
   * It does not conflict with names of real branches because the character ':'
   * is not accepted.
   */
  public static final String DEFAULT_KEY_OF_MAIN_BRANCH = ":main:";

  /**
   * Branch UUID is the projects.uuid that reference projects, branches or pull requests
   * (projects.qualifier="TRK").
   * Not null.
   * Important - the table project_branches does NOT have its own UUIDs for the time being.
   * All values must exist in projects.uuid.
   */
  private String uuid;

  /**
   * UUID of the project that represents the main branch.
   * On main branches, projectUuid equals uuid.
   * Not null.
   */
  private String projectUuid;

  /**
   * Not null.
   */
  private BranchKeyType keeType;

  /**
   * If {@link #keeType} is {@link BranchKeyType#BRANCH}, then name of branch, for example
   * "feature/foo". Can be {@link #DEFAULT_KEY_OF_MAIN_BRANCH} is the name is not known.
   *
   * If {@link #keeType} is {@link BranchKeyType#PR}, then id of the pull request, for
   * example "1204".
   */
  private String kee;

  /**
   * Value is mandatory when {@link #keeType} is {@link BranchKeyType#BRANCH}.
   * Otherwise it is null.
   */
  @Nullable
  private BranchType branchType;

  /**
   * UUID of the branch:
   * - in which the short-lived branch or pull request will be merged into
   * - that is the base of long-lived branch.
   *
   * Can be null if information is not known.
   */
  @Nullable
  private String mergeBranchUuid;

  /**
   * Optional title of pull requests
   */
  @Nullable
  private String pullRequestTitle;

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String s) {
    this.uuid = s;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public void setProjectUuid(String s) {
    this.projectUuid = s;
  }

  public BranchKeyType getKeeType() {
    return keeType;
  }

  public void setKeeType(BranchKeyType t) {
    this.keeType = t;
  }

  public String getKee() {
    return kee;
  }

  public void setKee(String s) {
    checkArgument(s.length() <= 255, "Maximum length of branch name or pull request id is 255: %s", s);
    this.kee = s;
  }

  @Nullable
  public BranchType getBranchType() {
    return branchType;
  }

  public void setBranchType(@Nullable BranchType b) {
    this.branchType = b;
  }

  @Nullable
  public String getMergeBranchUuid() {
    return mergeBranchUuid;
  }

  public void setMergeBranchUuid(@Nullable String s) {
    this.mergeBranchUuid = s;
  }

  @Nullable
  public String getPullRequestTitle() {
    return pullRequestTitle;
  }

  public void setPullRequestTitle(@Nullable String s) {
    checkArgument(s == null || s.length() <= 4000, "Maximum length of pull request title is 4000: %s", s);
    this.pullRequestTitle = s;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("BranchDto{");
    sb.append("uuid='").append(uuid).append('\'');
    sb.append(", mergeBranchUuid='").append(mergeBranchUuid).append('\'');
    sb.append(", pullRequestTitle='").append(pullRequestTitle).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
