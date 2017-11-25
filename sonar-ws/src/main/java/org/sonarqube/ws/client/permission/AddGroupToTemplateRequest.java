/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.permission;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class AddGroupToTemplateRequest {
  private String groupId;
  private String groupName;
  private String permission;
  private String templateId;
  private String templateName;

  @CheckForNull
  public String getGroupId() {
    return groupId;
  }

  public AddGroupToTemplateRequest setGroupId(@Nullable String groupId) {
    this.groupId = groupId;
    return this;
  }

  @CheckForNull
  public String getGroupName() {
    return groupName;
  }

  public AddGroupToTemplateRequest setGroupName(@Nullable String groupName) {
    this.groupName = groupName;
    return this;
  }

  public String getPermission() {
    return permission;
  }

  public AddGroupToTemplateRequest setPermission(String permission) {
    this.permission = requireNonNull(permission, "permission must not be null");
    return this;
  }

  @CheckForNull
  public String getTemplateId() {
    return templateId;
  }

  public AddGroupToTemplateRequest setTemplateId(@Nullable String templateId) {
    this.templateId = templateId;
    return this;
  }

  @CheckForNull
  public String getTemplateName() {
    return templateName;
  }

  public AddGroupToTemplateRequest setTemplateName(@Nullable String templateName) {
    this.templateName = templateName;
    return this;
  }
}
