/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.common.almsettings;

import javax.annotation.Nullable;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;

public record DevOpsProjectCreationContext(String name, String fullName, String devOpsPlatformIdentifier, @Nullable String url, @Nullable String repoId, boolean isPublic,
                                           @Nullable String defaultBranchName, AlmSettingDto almSettingDto, UserSession userSession) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String name;
    private String fullName;
    private String devOpsPlatformIdentifier;
    private String url;
    private String repoId;
    private boolean isPublic;
    private String defaultBranchName;
    private AlmSettingDto almSettingDto;
    private UserSession userSession;

    private Builder() {
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder fullName(String fullName) {
      this.fullName = fullName;
      return this;
    }

    public Builder devOpsPlatformIdentifier(String devOpsPlatformIdentifier) {
      this.devOpsPlatformIdentifier = devOpsPlatformIdentifier;
      return this;
    }

    public Builder url(@Nullable String url) {
      this.url = url;
      return this;
    }

    public Builder repoId(@Nullable String repoId) {
      this.repoId = repoId;
      return this;
    }

    public Builder isPublic(boolean isPublic) {
      this.isPublic = isPublic;
      return this;
    }

    public Builder defaultBranchName(@Nullable String defaultBranchName) {
      this.defaultBranchName = defaultBranchName;
      return this;
    }

    public Builder almSettingDto(AlmSettingDto almSettingDto) {
      this.almSettingDto = almSettingDto;
      return this;
    }

    public Builder userSession(UserSession userSession) {
      this.userSession = userSession;
      return this;
    }

    public DevOpsProjectCreationContext build() {
      return new DevOpsProjectCreationContext(name, fullName, devOpsPlatformIdentifier, url, repoId, isPublic, defaultBranchName, almSettingDto, userSession);
    }
  }
}
