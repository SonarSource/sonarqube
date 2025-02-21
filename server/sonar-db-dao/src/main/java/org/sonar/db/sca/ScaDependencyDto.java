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
package org.sonar.db.sca;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a Software Composition Analysis (SCA) dependency, associated with a component.
 * The component will be a package component nested inside a project branch component.
 *
 * One of userDependencyFilePath or lockfileDependencyFilePath should not be null.
 *
 * A dependency is a "mention" of a release in a project, with a scope and a specific
 * dependency file that it was mentioned in.
 *
 * @param uuid                       primary key
 * @param scaReleaseUuid             the UUID of the SCA release that this dependency refers to
 * @param direct                     is this a direct dependency of the project
 * @param scope                      the scope of the dependency e.g. "development"
 * @param userDependencyFilePath     path to the user-editable file where the dependency was found ("manifest") e.g. package.json
 * @param lockfileDependencyFilePath path to the machine-maintained lockfile where the dependency was found e.g. package-lock.json
 * @param chains                     a list of the purl chains that require the dependency, stored as JSON string, e.g. [["pkg:npm/foo@1.0.0", ...], ...]
 * @param createdAt                  timestamp of creation
 * @param updatedAt                  timestamp of most recent update
 */
public record ScaDependencyDto(
  String uuid,
  String scaReleaseUuid,
  boolean direct,
  String scope,
  @Nullable String userDependencyFilePath,
  @Nullable String lockfileDependencyFilePath,
  @Nullable List<List<String>> chains,
  long createdAt,
  long updatedAt) {

  // These need to be in sync with the database but because the db migration module and this module don't
  // depend on each other, we can't make one just refer to the other.
  public static final int SCOPE_MAX_LENGTH = 100;
  public static final int DEPENDENCY_FILE_PATH_MAX_LENGTH = 1000;

  private static final Gson GSON = new Gson();
  private static final TypeToken<List<List<String>>> CHAINS_TYPE = new TypeToken<>() {
  };

  public ScaDependencyDto {
    // We want these to raise errors and not silently put junk values in the db
    checkLength(scope, SCOPE_MAX_LENGTH, "scope");
    checkLength(userDependencyFilePath, DEPENDENCY_FILE_PATH_MAX_LENGTH, "userDependencyFilePath");
    checkLength(lockfileDependencyFilePath, DEPENDENCY_FILE_PATH_MAX_LENGTH, "lockfileDependencyFilePath");
    if (userDependencyFilePath == null && lockfileDependencyFilePath == null) {
      throw new IllegalArgumentException("One of userDependencyFilePath or lockfileDependencyFilePath should not be null");
    }
  }

  public String getChainsJson() {
    return chains == null ? null : GSON.toJson(chains);
  }

  /**
   * Returns the userDependencyFilePath if it is not null, otherwise returns the lockfileDependencyFilePath.
   *
   * @return a non-null file path
   */
  public String primaryDependencyFilePath() {
    return userDependencyFilePath != null ? userDependencyFilePath : lockfileDependencyFilePath;
  }

  private static void checkLength(@Nullable String value, int maxLength, String name) {
    if (value != null) {
      checkArgument(value.length() <= maxLength, "Maximum length of %s is %s: %s", name, maxLength, value);
    }
  }

  public static class Builder {
    private String uuid;
    private String scaReleaseUuid;
    private boolean direct;
    private String scope;
    private String userDependencyFilePath;
    private String lockfileDependencyFilePath;
    private List<List<String>> chains;
    private long createdAt;
    private long updatedAt;

    public Builder setUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder setScaReleaseUuid(String scaReleaseUuid) {
      this.scaReleaseUuid = scaReleaseUuid;
      return this;
    }

    public Builder setDirect(boolean direct) {
      this.direct = direct;
      return this;
    }

    public Builder setScope(String scope) {
      this.scope = scope;
      return this;
    }

    public Builder setUserDependencyFilePath(String dependencyFilePath) {
      this.userDependencyFilePath = dependencyFilePath;
      return this;
    }

    public Builder setLockfileDependencyFilePath(String dependencyFilePath) {
      this.lockfileDependencyFilePath = dependencyFilePath;
      return this;
    }

    public Builder setChains(List<List<String>> chains) {
      this.chains = chains;
      return this;
    }

    public Builder setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder setUpdatedAt(long updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public ScaDependencyDto build() {
      return new ScaDependencyDto(
        uuid, scaReleaseUuid, direct, scope, userDependencyFilePath, lockfileDependencyFilePath, chains, createdAt, updatedAt);
    }
  }

  public Builder toBuilder() {
    return new Builder()
      .setUuid(this.uuid)
      .setScaReleaseUuid(this.scaReleaseUuid)
      .setDirect(this.direct)
      .setScope(this.scope)
      .setUserDependencyFilePath(this.userDependencyFilePath)
      .setLockfileDependencyFilePath(this.lockfileDependencyFilePath)
      .setChains(this.chains)
      .setCreatedAt(this.createdAt)
      .setUpdatedAt(this.updatedAt);
  }
}
