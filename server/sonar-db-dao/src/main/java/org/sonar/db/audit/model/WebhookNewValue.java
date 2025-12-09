/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.function.UnaryOperator;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.webhook.WebhookDto;

public class WebhookNewValue extends NewValue {
  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @Nullable
  private String webhookUuid;

  @Nullable
  private String name;

  @Nullable
  private String url;

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

  public WebhookNewValue(WebhookDto dto, @Nullable String projectKey, @Nullable String projectName) {
    this(dto.getUuid(), dto.getName(), dto.getProjectUuid(), projectKey, projectName, dto.getUrl());
  }

  public WebhookNewValue(String webhookUuid, String webhookName) {
    this.webhookUuid = webhookUuid;
    this.name = webhookName;
  }

  public WebhookNewValue(String webhookUuid, String webhookName, @Nullable String projectUuid, @Nullable String projectKey,
    @Nullable String projectName, @Nullable String url) {
    this.webhookUuid = webhookUuid;
    this.name = webhookName;
    this.url = url;
    this.projectUuid = projectUuid;
    this.projectKey = projectKey;
    this.projectName = projectName;
  }

  public WebhookNewValue(ProjectDto projectDto) {
    this.projectUuid = projectDto.getUuid();
    this.projectKey = projectDto.getKey();
    this.projectName = projectDto.getName();
  }

  public void sanitizeUrl(UnaryOperator<String> sanitizer) {
    if (this.url != null) {
      this.url = sanitizer.apply(this.url);
    }
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  @CheckForNull
  public String getWebhookUuid() {
    return this.webhookUuid;
  }

  @CheckForNull
  public String getName() {
    return this.name;
  }

  @CheckForNull
  public String getUrl() {
    return this.url;
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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"webhookUuid\": ", this.webhookUuid, true);
    addField(sb, "\"name\": ", this.name, true);
    addField(sb, "\"url\": ", this.url, true);
    addField(sb, "\"projectUuid\": ", this.projectUuid, true);
    addField(sb, "\"projectKey\": ", this.projectKey, true);
    addField(sb, "\"projectName\": ", this.projectName, true);
    endString(sb);
    return sb.toString();
  }
}
