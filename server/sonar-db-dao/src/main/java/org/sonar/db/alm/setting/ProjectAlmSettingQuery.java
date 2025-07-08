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
package org.sonar.db.alm.setting;

import javax.annotation.Nullable;

public record ProjectAlmSettingQuery(
  @Nullable String repository,
  @Nullable String almSettingUuid,
  @Nullable String almRepo,
  @Nullable String almSlug) {

  // Existing constructor for backward compatibility (repository search in both alm_repo and alm_slug)
  public ProjectAlmSettingQuery(String repository, String almSettingUuid) {
    this(repository, almSettingUuid, null, null);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String repository;
    private String almSettingUuid;
    private String almRepo;
    private String almSlug;

    private Builder() {
    }

    public Builder repository(String repository) {
      if (almRepo != null || almSlug != null) {
        throw new IllegalStateException("Cannot use repository with almRepo or almSlug");
      }
      this.repository = repository;
      return this;
    }

    public Builder almSettingUuid(String almSettingUuid) {
      if (almRepo != null || almSlug != null) {
        throw new IllegalStateException("Cannot use almSettingUuid with almRepo or almSlug");
      }
      this.almSettingUuid = almSettingUuid;
      return this;
    }

    public Builder almRepo(String almRepo) {
      if (repository != null || almSettingUuid != null) {
        throw new IllegalStateException("Cannot use almRepo with repository or almSettingUuid");
      }
      this.almRepo = almRepo;
      return this;
    }

    public Builder almSlug(String almSlug) {
      if (repository != null || almSettingUuid != null) {
        throw new IllegalStateException("Cannot use almSlug with repository or almSettingUuid");
      }
      this.almSlug = almSlug;
      return this;
    }

    public ProjectAlmSettingQuery build() {
      return new ProjectAlmSettingQuery(repository, almSettingUuid, almRepo, almSlug);
    }
  }

  public static ProjectAlmSettingQuery forAlmRepo(String almRepo) {
    return builder().almRepo(almRepo).build();
  }

  public static ProjectAlmSettingQuery forAlmRepoAndSlug(String almRepo, String almSlug) {
    return builder().almRepo(almRepo).almSlug(almSlug).build();
  }

}
