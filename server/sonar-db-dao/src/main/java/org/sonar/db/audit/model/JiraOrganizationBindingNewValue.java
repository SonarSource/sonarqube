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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class JiraOrganizationBindingNewValue extends NewValue {

  @Nullable
  private String bindingId;

  @Nullable
  private String sonarOrganizationUuid;

  @Nullable
  private String jiraCloudId;

  @Nullable
  private String jiraInstanceUrl;

  private boolean isTokenShared;

  public JiraOrganizationBindingNewValue(String bindingId, String sonarOrganizationUuid, String jiraCloudId, String jiraInstanceUrl, boolean isTokenShared) {
    this.bindingId = bindingId;
    this.sonarOrganizationUuid = sonarOrganizationUuid;
    this.jiraCloudId = jiraCloudId;
    this.jiraInstanceUrl = jiraInstanceUrl;
    this.isTokenShared = isTokenShared;
  }

  @CheckForNull
  public String getBindingId() {
    return this.bindingId;
  }

  @CheckForNull
  public String getSonarOrganizationUuid() {
    return this.sonarOrganizationUuid;
  }

  @CheckForNull
  public String getJiraCloudId() {
    return this.jiraCloudId;
  }

  @CheckForNull
  public String getJiraInstanceUrl() {
    return this.jiraInstanceUrl;
  }

  public boolean getIsTokenShared() {
    return this.isTokenShared;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"bindingId\": ", this.bindingId, true);
    addField(sb, "\"sonarOrganizationUuid\": ", this.sonarOrganizationUuid, true);
    addField(sb, "\"jiraCloudId\": ", this.jiraCloudId, true);
    addField(sb, "\"jiraInstanceUrl\": ", this.jiraInstanceUrl, true);
    addField(sb, "\"isTokenShared\": ", Boolean.toString(this.isTokenShared), false);
    endString(sb);
    return sb.toString();
  }
}

