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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.StringUtils.repeat;

public class BranchDto {

  /**
   * Maximum length of column "kee"
   */
  public static final int KEE_MAX_LENGTH = 255;

  /**
   * Value of {@link #kee} when the name of main branch is not known.
   * Used only if {@link #keeType} is {@link BranchKeyType#BRANCH}.
   * It does not conflict with names of real branches because the term ':BRANCH:'
   * is not accepted.
   */
  public static final String NULL_KEY = repeat("_", KEE_MAX_LENGTH);

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
   * "feature/foo". Can be {@link #NULL_KEY} is the name is not known.
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

  public BranchDto setUuid(String s) {
    this.uuid = s;
    return this;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public BranchDto setProjectUuid(String s) {
    this.projectUuid = s;
    return this;
  }

  public boolean isMain() {
    return projectUuid.equals(uuid);
  }

  public BranchKeyType getKeeType() {
    return keeType;
  }

  public BranchDto setKeeType(BranchKeyType t) {
    this.keeType = t;
    return this;
  }

  /**
   * This is the getter used by MyBatis mapper. It does
   * not handle the special value used to map null field.
   */
  private String getKee() {
    return kee;
  }

  @CheckForNull
  public String getKey() {
    return convertKeyFromDb(getKee());
  }

  /**
   * This is the setter used by MyBatis mapper. It does
   * not handle the special value used to map null field.
   */
  private void setKee(String s) {
    this.kee = s;
  }

  public BranchDto setKey(@Nullable String s) {
    checkArgument(s == null || s.length() <= KEE_MAX_LENGTH, "Maximum length of branch name or pull request id is %s: %s", KEE_MAX_LENGTH, s);
    checkArgument(!NULL_KEY.equals(s), "Branch name is not allowed: %s", s);
    setKee(convertKeyToDb(s));
    return this;
  }

  @Nullable
  public BranchType getBranchType() {
    return branchType;
  }

  public BranchDto setBranchType(@Nullable BranchType b) {
    this.branchType = b;
    return this;
  }

  @Nullable
  public String getMergeBranchUuid() {
    return mergeBranchUuid;
  }

  public BranchDto setMergeBranchUuid(@Nullable String s) {
    this.mergeBranchUuid = s;
    return this;
  }

  @Nullable
  public String getPullRequestTitle() {
    return pullRequestTitle;
  }

  public BranchDto setPullRequestTitle(@Nullable String s) {
    checkArgument(s == null || s.length() <= 4000, "Maximum length of pull request title is 4000: %s", s);
    this.pullRequestTitle = s;
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("BranchDto{");
    sb.append("uuid='").append(uuid).append('\'');
    sb.append(", projectUuid='").append(projectUuid).append('\'');
    sb.append(", keeType=").append(keeType);
    sb.append(", kee='").append(kee).append('\'');
    sb.append(", branchType=").append(branchType);
    sb.append(", mergeBranchUuid='").append(mergeBranchUuid).append('\'');
    sb.append(", pullRequestTitle='").append(pullRequestTitle).append('\'');
    sb.append('}');
    return sb.toString();
  }

  static String convertKeyToDb(@Nullable String s) {
    return s == null ? NULL_KEY : s;
  }

  @CheckForNull
  static String convertKeyFromDb(String s) {
    return NULL_KEY.equals(s) ? null : s;
  }

}
