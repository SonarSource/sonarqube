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

public class PermissionTemplateUser {

  private final Long templateId;
  private final Long userId;
  private final String userName;
  private final String userLogin;
  private final String permission;

  public PermissionTemplateUser(Long templateId, Long userId, String userName, String userLogin, String permission) {
    this.templateId = templateId;
    this.userId = userId;
    this.userName = userName;
    this.userLogin = userLogin;
    this.permission = permission;
  }

  public Long getUserId() {
    return userId;
  }

  public String getUserName() {
    return userName;
  }

  public String getPermission() {
    return permission;
  }

  public Long getTemplateId() {
    return templateId;
  }

  public String getUserLogin() {
    return userLogin;
  }
}
