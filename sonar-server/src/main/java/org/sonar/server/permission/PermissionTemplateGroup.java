/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.permission;

public class PermissionTemplateGroup {

  private final Long templateId;
  private final Long groupId;
  private final String groupName;
  private final String permission;

  public PermissionTemplateGroup(Long templateId, Long groupId, String groupName, String permission) {
    this.templateId = templateId;
    this.groupId = groupId;
    this.groupName = groupName;
    this.permission = permission;
  }

  public Long getTemplateId() {
    return templateId;
  }

  public Long getGroupId() {
    return groupId;
  }

  public String getGroupName() {
    return groupName;
  }

  public String getPermission() {
    return permission;
  }
}
