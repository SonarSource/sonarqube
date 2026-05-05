/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.adminalert;

import javax.annotation.Nullable;

public class AdminAlertStatusDto {

  private String uuid;
  private String alertKey;
  private boolean active;
  @Nullable
  private Long activatedAt;
  @Nullable
  private Long deactivatedAt;
  private long updatedAt;

  public AdminAlertStatusDto() {
    // nothing to do here
  }

  public String getUuid() {
    return uuid;
  }

  public AdminAlertStatusDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getAlertKey() {
    return alertKey;
  }

  public AdminAlertStatusDto setAlertKey(String alertKey) {
    this.alertKey = alertKey;
    return this;
  }

  public boolean isActive() {
    return active;
  }

  public AdminAlertStatusDto setActive(boolean active) {
    this.active = active;
    return this;
  }

  @Nullable
  public Long getActivatedAt() {
    return activatedAt;
  }

  public AdminAlertStatusDto setActivatedAt(@Nullable Long activatedAt) {
    this.activatedAt = activatedAt;
    return this;
  }

  @Nullable
  public Long getDeactivatedAt() {
    return deactivatedAt;
  }

  public AdminAlertStatusDto setDeactivatedAt(@Nullable Long deactivatedAt) {
    this.deactivatedAt = deactivatedAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public AdminAlertStatusDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
