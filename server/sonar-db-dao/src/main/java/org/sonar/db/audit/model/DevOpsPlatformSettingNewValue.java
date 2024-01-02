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
package org.sonar.db.audit.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;

public class DevOpsPlatformSettingNewValue extends NewValue {

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @Nullable
  private String devOpsPlatformSettingUuid;

  @Nullable
  private String key;

  @Nullable
  private String devOpsPlatformName;

  @Nullable
  private String url;

  @Nullable
  private String appId;

  @Nullable
  private String clientId;

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @Nullable
  private String projectUuid;

  @Nullable
  private String projectKey;

  @Nullable
  private String projectName;

  @Nullable
  private String almRepo;

  @Nullable
  private String almSlug;

  @Nullable
  private Boolean isSummaryCommentEnabled;

  @Nullable
  private Boolean isMonorepo;

  public DevOpsPlatformSettingNewValue(String devOpsPlatformSettingUuid, String key) {
    this.devOpsPlatformSettingUuid = devOpsPlatformSettingUuid;
    this.key = key;
  }

  public DevOpsPlatformSettingNewValue(AlmSettingDto dto) {
    this.devOpsPlatformSettingUuid = dto.getUuid();
    this.key = dto.getKey();
    this.devOpsPlatformName = dto.getAppId();
    this.url = dto.getUrl();
    this.appId = dto.getAppId();
    this.clientId = dto.getClientId();
  }

  public DevOpsPlatformSettingNewValue(ProjectAlmSettingDto dto, String key, String projectName, String projectKey) {
    this.devOpsPlatformSettingUuid = dto.getAlmSettingUuid();
    this.key = key;
    this.projectUuid = dto.getProjectUuid();
    this.projectKey = projectKey;
    this.projectName = projectName;
    this.almRepo = dto.getAlmRepo();
    this.almSlug = dto.getAlmSlug();
    this.isSummaryCommentEnabled = dto.getSummaryCommentEnabled();
    this.isMonorepo = dto.getMonorepo();
  }

  public DevOpsPlatformSettingNewValue(ProjectDto projectDto) {
    this.projectUuid = projectDto.getUuid();
    this.projectKey = projectDto.getKey();
    this.projectName = projectDto.getName();
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @CheckForNull
  public String getDevOpsPlatformSettingUuid() {
    return this.devOpsPlatformSettingUuid;
  }

  @CheckForNull
  public String getKey() {
    return this.key;
  }

  @CheckForNull
  public String getDevOpsPlatformName() {
    return this.devOpsPlatformName;
  }

  @CheckForNull
  public String getUrl() {
    return this.url;
  }

  @CheckForNull
  public String getAppId() {
    return this.appId;
  }

  @CheckForNull
  public String getClientId() {
    return this.clientId;
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @CheckForNull
  public String getProjectUuid() {
    return this.projectUuid;
  }

  @CheckForNull
  public String getProjectKey() {
    return this.projectKey;
  }

  @CheckForNull
  public String getProjectName() {
    return this.projectName;
  }

  @CheckForNull
  public String getAlmRepo() {
    return this.almRepo;
  }

  @CheckForNull
  public String getAlmSlug() {
    return this.almSlug;
  }

  @CheckForNull
  public Boolean isSummaryCommentEnabled() {
    return this.isSummaryCommentEnabled;
  }

  @CheckForNull
  public Boolean isMonorepo() {
    return this.isMonorepo;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"devOpsPlatformSettingUuid\": ", this.devOpsPlatformSettingUuid, true);
    addField(sb, "\"key\": ", this.key, true);
    addField(sb, "\"devOpsPlatformName\": ", this.devOpsPlatformName, true);
    addField(sb, "\"url\": ", this.url, true);
    addField(sb, "\"appId\": ", this.appId, true);
    addField(sb, "\"clientId\": ", this.clientId, true);
    addField(sb, "\"projectUuid\": ", this.projectUuid, true);
    addField(sb, "\"projectKey\": ", this.projectKey, true);
    addField(sb, "\"projectName\": ", this.projectName, true);
    addField(sb, "\"almRepo\": ", this.almRepo, true);
    addField(sb, "\"almSlug\": ", this.almSlug, true);
    addField(sb, "\"isSummaryCommentEnabled\": ", ObjectUtils.toString(this.isSummaryCommentEnabled), false);
    addField(sb, "\"isMonorepo\": ", ObjectUtils.toString(this.isMonorepo), false);
    endString(sb);
    return sb.toString();
  }
}
