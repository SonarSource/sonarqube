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
package org.sonar.db.component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.protobuf.DbProjectBranches;

import static com.google.common.base.Preconditions.checkArgument;

public class BranchDto {
  public static final String DEFAULT_MAIN_BRANCH_NAME = "master";

  /**
   * Maximum length of column "kee"
   */
  public static final int KEE_MAX_LENGTH = 255;

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
   * Key that identifies a branch or a pull request.
   * For keyType=BRANCH, this is the name of the branch, for example "feature/foo".
   * For keyType=PULL_REQUEST, this is the ID of the pull request in some external system, for example 123 in GitHub.
   */
  private String kee;

  /**
   * Key type, as provided by {@link KeyType}.
   * Not null.
   */
  private KeyType keyType;

  /**
   * Branch type, as provided by {@link BranchType}.
   * Not null.
   */
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
   * Pull Request data, such as branch name, title, url, and provider specific attributes
   */
  @Nullable
  private byte[] pullRequestBinary;

  /**
   * The UUID of the analysis set by user as manual baseline for computation of the New Code Period, if any.
   */
  @Nullable
  private String manualBaseline;

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

  /**
   * This is the getter used by MyBatis mapper.
   */
  private String getKee() {
    return kee;
  }

  public String getKey() {
    return kee;
  }

  /**
   * This is the setter used by MyBatis mapper.
   */
  private void setKee(String s) {
    this.kee = s;
  }

  public BranchDto setKey(String s) {
    checkArgument(s.length() <= KEE_MAX_LENGTH, "Maximum length of branch name or pull request id is %s: %s", KEE_MAX_LENGTH, s);
    setKee(s);
    return this;
  }

  BranchDto setKeyType(KeyType keyType) {
    this.keyType = keyType;
    return this;
  }

  public BranchType getBranchType() {
    return branchType;
  }

  public BranchDto setBranchType(BranchType b) {
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

  public BranchDto setPullRequestData(DbProjectBranches.PullRequestData pullRequestData) {
    this.pullRequestBinary = encodePullRequestData(pullRequestData);
    return this;
  }

  @CheckForNull
  public DbProjectBranches.PullRequestData getPullRequestData() {
    if (pullRequestBinary == null) {
      return null;
    }
    return decodePullRequestData(pullRequestBinary);
  }

  private static byte[] encodePullRequestData(DbProjectBranches.PullRequestData pullRequestData) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      pullRequestData.writeTo(outputStream);
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Fail to serialize pull request data", e);
    }
  }

  private static DbProjectBranches.PullRequestData decodePullRequestData(byte[] pullRequestBinary) {
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pullRequestBinary)) {
      return DbProjectBranches.PullRequestData.parseFrom(inputStream);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to deserialize pull request data", e);
    }
  }

  @CheckForNull
  public String getManualBaseline() {
    return manualBaseline;
  }

  public BranchDto setManualBaseline(@Nullable String manualBaseline) {
    this.manualBaseline = manualBaseline == null || manualBaseline.isEmpty() ? null : manualBaseline;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BranchDto branchDto = (BranchDto) o;
    return Objects.equals(uuid, branchDto.uuid) &&
        Objects.equals(projectUuid, branchDto.projectUuid) &&
        Objects.equals(kee, branchDto.kee) &&
        branchType == branchDto.branchType &&
        Objects.equals(mergeBranchUuid, branchDto.mergeBranchUuid) &&
        Objects.equals(manualBaseline, branchDto.manualBaseline);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, projectUuid, kee, branchType, mergeBranchUuid);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("BranchDto{");
    sb.append("uuid='").append(uuid).append('\'');
    sb.append(", projectUuid='").append(projectUuid).append('\'');
    sb.append(", kee='").append(kee).append('\'');
    sb.append(", keyType=").append(keyType);
    sb.append(", branchType=").append(branchType);
    sb.append(", mergeBranchUuid='").append(mergeBranchUuid).append('\'');
    sb.append(", manualBaseline='").append(manualBaseline).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
