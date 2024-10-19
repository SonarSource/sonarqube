/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ProjectAlmSettingDto {

  /**
   * Not empty. Max size is 40. Obviously it is unique.
   */
  private String uuid;

  /**
   * Non-null UUID of project. Max size is 40.
   * @see org.sonar.db.entity.EntityDto#getUuid()
   */
  private String projectUuid;

  /**
   * Non-null UUID of the ALM Setting UUID. Max size is 40.
   * @see AlmSettingDto#getUuid()
   */
  private String almSettingUuid;

  /**
   * Identifier of the repository in the ALM. Max size is 256.
   * This column will only be fed when alm is GitHub or Bitbucket.
   * It will be null when the ALM is Azure DevOps.
   */
  private String almRepo;

  /**
   * Slug of the repository in the ALM. Max size is 256.
   * This column will only be fed when alm is Bitbucket.
   * It will be null when the ALM is Azure DevOps, or GitHub.
   */
  private String almSlug;

  /**
   * Boolean flag which enable/disable inserting summary of analysis as a comment
   * It will be null when the ALM is other than GitHub
   */
  private Boolean summaryCommentEnabled;

  /**
   * Boolean to know if this SonarQube project is part of a monorepo
   * default value is false
   */
  private Boolean monorepo;

  private long updatedAt;
  private long createdAt;

  public String getUuid() {
    return uuid;
  }

  void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public ProjectAlmSettingDto setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  public String getAlmSettingUuid() {
    return almSettingUuid;
  }

  public ProjectAlmSettingDto setAlmSettingUuid(String almSettingUuid) {
    this.almSettingUuid = almSettingUuid;
    return this;
  }

  @CheckForNull
  public String getAlmRepo() {
    return almRepo;
  }

  public ProjectAlmSettingDto setAlmRepo(@Nullable String almRepo) {
    this.almRepo = almRepo;
    return this;
  }

  @CheckForNull
  public String getAlmSlug() {
    return almSlug;
  }

  public ProjectAlmSettingDto setAlmSlug(@Nullable String almSlug) {
    this.almSlug = almSlug;
    return this;
  }

  public Boolean getSummaryCommentEnabled() {
    return summaryCommentEnabled;
  }

  public ProjectAlmSettingDto setSummaryCommentEnabled(@Nullable Boolean summaryCommentEnabled) {
    this.summaryCommentEnabled = summaryCommentEnabled;
    return this;
  }

  public Boolean getMonorepo() {
    return monorepo;
  }

  public ProjectAlmSettingDto setMonorepo(Boolean monorepo) {
    this.monorepo = monorepo;
    return this;
  }

  long getUpdatedAt() {
    return updatedAt;
  }

  void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }

  long getCreatedAt() {
    return createdAt;
  }

  void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }
}
