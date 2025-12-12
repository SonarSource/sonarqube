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

public class JiraProjectBindingNewValue extends NewValue {

  @Nullable
  private String bindingId;

  @Nullable
  private String sonarProjectId;

  @Nullable
  private String jiraOrganizationBindingId;

  @Nullable
  private String jiraProjectKey;

  public JiraProjectBindingNewValue(String bindingId, String sonarProjectId, String jiraOrganizationBindingId, String jiraProjectKey) {
    this.bindingId = bindingId;
    this.sonarProjectId = sonarProjectId;
    this.jiraOrganizationBindingId = jiraOrganizationBindingId;
    this.jiraProjectKey = jiraProjectKey;
  }

  @CheckForNull
  public String getBindingId() {
    return this.bindingId;
  }

  @CheckForNull
  public String getSonarProjectId() {
    return this.sonarProjectId;
  }

  @CheckForNull
  public String getJiraOrganizationBindingId() {
    return this.jiraOrganizationBindingId;
  }

  @CheckForNull
  public String getJiraProjectKey() {
    return this.jiraProjectKey;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"bindingId\": ", this.bindingId, true);
    addField(sb, "\"sonarProjectId\": ", this.sonarProjectId, true);
    addField(sb, "\"jiraOrganizationBindingId\": ", this.jiraOrganizationBindingId, true);
    addField(sb, "\"jiraProjectKey\": ", this.jiraProjectKey, true);
    endString(sb);
    return sb.toString();
  }
}

