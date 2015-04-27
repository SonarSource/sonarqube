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

package org.sonar.core.persistence.migration.v50;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class Component {

  private Long id;
  private Long projectId;
  private Long snapshotId;
  private String snapshotPath;
  private String scope;

  private String uuid;
  private String projectUuid;
  private String moduleUuid;
  private String moduleUuidPath = "";

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Can be null on provisioned projects or library
   */
  @CheckForNull
  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(@Nullable Long projectId) {
    this.projectId = projectId;
  }

  /**
   * Can be null on provisioned projects or library
   */
  @CheckForNull
  public Long getSnapshotId() {
    return snapshotId;
  }

  public void setSnapshotId(@Nullable Long snapshotId) {
    this.snapshotId = snapshotId;
  }

  @CheckForNull
  public String getSnapshotPath() {
    return snapshotPath;
  }

  public void setSnapshotPath(@Nullable String snapshotPath) {
    this.snapshotPath = snapshotPath;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public void setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
  }

  @CheckForNull
  public String getModuleUuid() {
    return moduleUuid;
  }

  public void setModuleUuid(@Nullable String moduleUuid) {
    this.moduleUuid = moduleUuid;
  }

  @CheckForNull
  public String getModuleUuidPath() {
    return moduleUuidPath;
  }

  public void setModuleUuidPath(@Nullable String moduleUuidPath) {
    this.moduleUuidPath = moduleUuidPath;
  }
}
