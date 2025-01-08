/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.component;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public final class SnapshotQuery {

  public enum SORT_FIELD {
    BY_DATE("created_at");
    final String fieldName;

    SORT_FIELD(String fieldName) {
      this.fieldName = fieldName;
    }
  }

  public enum SORT_ORDER {
    ASC("asc"), DESC("desc");
    final String order;

    SORT_ORDER(String order) {
      this.order = order;
    }
  }

  private String rootComponentUuid;
  private Long createdAfter;
  private Long createdBefore;
  private List<String> statuses;
  private String projectVersion;
  private Boolean isLast;
  private String sortField;
  private String sortOrder;

  /**
   * filter to return snapshots created at or after a given date
   */
  @CheckForNull
  public Long getCreatedAfter() {
    return createdAfter;
  }

  public SnapshotQuery setCreatedAfter(@Nullable Long createdAfter) {
    this.createdAfter = createdAfter;
    return this;
  }

  /**
   * filter to return snapshots created before a given date
   */
  @CheckForNull
  public Long getCreatedBefore() {
    return createdBefore;
  }

  public SnapshotQuery setCreatedBefore(@Nullable Long createdBefore) {
    this.createdBefore = createdBefore;
    return this;
  }

  @CheckForNull
  public Boolean getIsLast() {
    return isLast;
  }

  public SnapshotQuery setIsLast(@Nullable Boolean isLast) {
    this.isLast = isLast;
    return this;
  }

  @CheckForNull
  public String getRootComponentUuid() {
    return rootComponentUuid;
  }

  public SnapshotQuery setRootComponentUuid(@Nullable String rootComponentUuid) {
    this.rootComponentUuid = rootComponentUuid;
    return this;
  }

  @CheckForNull
  public List<String> getStatus() {
    return statuses;
  }

  public SnapshotQuery setStatus(@Nullable String status) {
    this.statuses = List.of(status);
    return this;
  }

  public SnapshotQuery setStatuses(@Nullable List<String> statuses) {
    this.statuses = statuses;
    return this;
  }

  @CheckForNull
  public String getProjectVersion() {
    return projectVersion;
  }

  public SnapshotQuery setProjectVersion(@Nullable String projectVersion) {
    this.projectVersion = projectVersion;
    return this;
  }

  public SnapshotQuery setSort(SORT_FIELD sortField, SORT_ORDER sortOrder) {
    this.sortField = sortField.fieldName;
    this.sortOrder = sortOrder.order;
    return this;
  }

  @CheckForNull
  public String getSortField() {
    return sortField;
  }

  @CheckForNull
  public String getSortOrder() {
    return sortOrder;
  }
}
