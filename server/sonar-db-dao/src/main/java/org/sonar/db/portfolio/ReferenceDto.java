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
package org.sonar.db.portfolio;

import java.util.Set;

public class ReferenceDto {
  private String sourceUuid;
  private String sourceRootUuid;
  private String targetUuid;
  private String targetRootUuid;
  private String targetName;
  private String targetKey;
  private Set<String> branchUuids;

  public String getTargetName() {
    return targetName;
  }

  public ReferenceDto setTargetName(String targetName) {
    this.targetName = targetName;
    return this;
  }

  public String getTargetKey() {
    return targetKey;
  }

  public ReferenceDto setTargetKey(String targetKey) {
    this.targetKey = targetKey;
    return this;
  }

  public Set<String> getBranchUuids() {
    return branchUuids;
  }

  public ReferenceDto setBranchUuids(Set<String> branchUuids) {
    this.branchUuids = branchUuids;
    return this;
  }

  public String getSourceUuid() {
    return sourceUuid;
  }

  public void setSourceUuid(String sourceUuid) {
    this.sourceUuid = sourceUuid;
  }

  public String getSourceRootUuid() {
    return sourceRootUuid;
  }

  public void setSourceRootUuid(String sourceRootUuid) {
    this.sourceRootUuid = sourceRootUuid;
  }

  public String getTargetUuid() {
    return targetUuid;
  }

  public void setTargetUuid(String targetUuid) {
    this.targetUuid = targetUuid;
  }

  public String getTargetRootUuid() {
    return targetRootUuid;
  }

  public void setTargetRootUuid(String targetRootUuid) {
    this.targetRootUuid = targetRootUuid;
  }

}
