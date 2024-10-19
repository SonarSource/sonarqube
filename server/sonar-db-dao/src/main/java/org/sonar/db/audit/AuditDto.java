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
package org.sonar.db.audit;

import javax.annotation.Nullable;

public class AuditDto {

  private String uuid;
  private String organizationUuid;
  private String userUuid;
  private String userLogin;
  private boolean userTriggered;
  private String category;
  private String operation;
  private String newValue;
  private long createdAt;

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public void setOrganizationUuid(String organizationUuid) {
    this.organizationUuid = organizationUuid;
  }

  public String getUserUuid() {
    return userUuid;
  }

  public void setUserUuid(@Nullable String userUuid) {
    this.userUuid = userUuid;
  }

  public String getUserLogin() {
    return userLogin;
  }

  public void setUserLogin(@Nullable String userLogin) {
    this.userLogin = userLogin;
  }

  public boolean isUserTriggered() {
    return userTriggered;
  }

  public void setUserTriggered(boolean userTriggered) {
    this.userTriggered = userTriggered;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public String getNewValue() {
    return newValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }
}
