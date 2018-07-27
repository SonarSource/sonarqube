/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

public class ProjectAlmBindingDto {
  private String uuid;
  private String almId;
  private String repoId;
  private String projectUuid;
  private String githubSlug;
  private String url;

  public String getAlmId() {
    return almId;
  }

  public ProjectAlmBindingDto setAlmId(String almId) {
    this.almId = almId;
    return this;
  }

  public String getRepoId() {
    return repoId;
  }

  public ProjectAlmBindingDto setRepoId(String repoId) {
    this.repoId = repoId;
    return this;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public ProjectAlmBindingDto setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  public String getGithubSlug() {
    return githubSlug;
  }

  public ProjectAlmBindingDto setGithubSlug(String githubSlug) {
    this.githubSlug = githubSlug;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public ProjectAlmBindingDto setUrl(String url) {
    this.url = url;
    return this;
  }

  public String getUuid() {
    return uuid;
  }

  public ProjectAlmBindingDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }
}
