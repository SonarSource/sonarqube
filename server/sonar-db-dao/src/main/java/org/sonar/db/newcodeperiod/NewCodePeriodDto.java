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
package org.sonar.db.newcodeperiod;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class NewCodePeriodDto {
  private String uuid = null;
  private String projectUuid = null;
  private String branchUuid = null;
  private NewCodePeriodType type = null;
  private String previousNonCompliantValue = null;
  private String value = null;
  private long updatedAt = 0L;
  private long createdAt = 0L;

  public static NewCodePeriodDto defaultInstance() {
    return new NewCodePeriodDto().setType(NewCodePeriodType.PREVIOUS_VERSION);
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public NewCodePeriodDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public NewCodePeriodDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getUuid() {
    return uuid;
  }

  public NewCodePeriodDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  @CheckForNull
  public String getProjectUuid() {
    return projectUuid;
  }

  public NewCodePeriodDto setProjectUuid(@Nullable String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  @CheckForNull
  public String getBranchUuid() {
    return branchUuid;
  }

  public NewCodePeriodDto setBranchUuid(@Nullable String branchUuid) {
    this.branchUuid = branchUuid;
    return this;
  }

  public NewCodePeriodType getType() {
    return type;
  }

  public NewCodePeriodDto setType(NewCodePeriodType type) {
    this.type = type;
    return this;
  }

  @CheckForNull
  public String getValue() {
    return value;
  }

  public NewCodePeriodDto setValue(@Nullable String value) {
    this.value = value;
    return this;
  }

  public String getPreviousNonCompliantValue() {
    return previousNonCompliantValue;
  }

  public NewCodePeriodDto setPreviousNonCompliantValue(String previousNonCompliantValue) {
    this.previousNonCompliantValue = previousNonCompliantValue;
    return this;
  }
}
