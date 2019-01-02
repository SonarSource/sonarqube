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
package org.sonar.db.alm;

import java.util.Arrays;

/**
 * DTO is used only for select, hence no setters (MyBatis populates field by reflection).
 */
public class ProjectAlmBindingDto {
  private String uuid;
  private String rawAlmId;
  private String repoId;
  private String projectUuid;
  private String githubSlug;
  private String url;

  public ALM getAlm() {
    return Arrays.stream(ALM.values())
      .filter(a -> a.getId().equals(rawAlmId))
      .findAny()
      .orElseThrow(() -> new IllegalStateException("ALM id " + rawAlmId + " is invalid"));
  }

  public String getRepoId() {
    return repoId;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public String getGithubSlug() {
    return githubSlug;
  }

  public String getUrl() {
    return url;
  }

  public String getUuid() {
    return uuid;
  }

}
