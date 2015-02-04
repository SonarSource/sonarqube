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
package org.sonar.core.component;

public final class SnapshotDto {

  /**
   * This status is set on the snapshot at the beginning of the batch
   */
  public static final String STATUS_UNPROCESSED = "U";
  public static final String STATUS_PROCESSED = "P";

  private static final String INDEX_SHOULD_BE_IN_RANGE_FROM_1_TO_5 = "Index should be in range from 1 to 5";

  private Long id;
  private Long parentId;
  private Long rootId;
  private Long rootProjectId;

  private Long createdAt;
  private Long buildDate;
  private Long resourceId;
  private String status = STATUS_UNPROCESSED;
  private Integer purgeStatus;
  private Boolean last;
  private String scope;
  private String qualifier;
  private String version;
  private String path;
  private Integer depth;

  private String period1Mode;
  private String period2Mode;
  private String period3Mode;
  private String period4Mode;
  private String period5Mode;

  private String period1Param;
  private String period2Param;
  private String period3Param;
  private String period4Param;
  private String period5Param;

  private Long period1Date;
  private Long period2Date;
  private Long period3Date;
  private Long period4Date;
  private Long period5Date;

  public Long getId() {
    return id;
  }

  public SnapshotDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getParentId() {
    return parentId;
  }

  public SnapshotDto setParentId(Long parentId) {
    this.parentId = parentId;
    return this;
  }

  public Long getRootId() {
    return rootId;
  }

  public SnapshotDto setRootId(Long rootId) {
    this.rootId = rootId;
    return this;
  }

  public Long getBuildDate() {
    return buildDate;
  }

  public SnapshotDto setBuildDate(Long buildDate) {
    this.buildDate = buildDate;
    return this;
  }

  public Long getResourceId() {
    return resourceId;
  }

  public SnapshotDto setResourceId(Long resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public SnapshotDto setStatus(String status) {
    this.status = status;
    return this;
  }

  public Integer getPurgeStatus() {
    return purgeStatus;
  }

  public SnapshotDto setPurgeStatus(Integer purgeStatus) {
    this.purgeStatus = purgeStatus;
    return this;
  }

  public Boolean getLast() {
    return last;
  }

  public SnapshotDto setLast(Boolean last) {
    this.last = last;
    return this;
  }

  public String getScope() {
    return scope;
  }

  public SnapshotDto setScope(String scope) {
    this.scope = scope;
    return this;
  }

  public String getQualifier() {
    return qualifier;
  }

  public SnapshotDto setQualifier(String qualifier) {
    this.qualifier = qualifier;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public SnapshotDto setVersion(String version) {
    this.version = version;
    return this;
  }

  public String getPath() {
    return path;
  }

  public SnapshotDto setPath(String path) {
    this.path = path;
    return this;
  }

  public Integer getDepth() {
    return depth;
  }

  public SnapshotDto setDepth(Integer depth) {
    this.depth = depth;
    return this;
  }

  public Long getRootProjectId() {
    return rootProjectId;
  }

  public SnapshotDto setRootProjectId(Long rootProjectId) {
    this.rootProjectId = rootProjectId;
    return this;
  }

  public SnapshotDto setPeriodMode(int index, String p) {
    switch (index) {
      case 1:
        period1Mode = p;
        break;
      case 2:
        period2Mode = p;
        break;
      case 3:
        period3Mode = p;
        break;
      case 4:
        period4Mode = p;
        break;
      case 5:
        period5Mode = p;
        break;
      default:
        throw new IndexOutOfBoundsException(INDEX_SHOULD_BE_IN_RANGE_FROM_1_TO_5);
    }
    return this;
  }

  public String getPeriodMode(int index) {
    switch (index) {
      case 1:
        return period1Mode;
      case 2:
        return period2Mode;
      case 3:
        return period3Mode;
      case 4:
        return period4Mode;
      case 5:
        return period5Mode;
      default:
        throw new IndexOutOfBoundsException(INDEX_SHOULD_BE_IN_RANGE_FROM_1_TO_5);
    }
  }

  public SnapshotDto setPeriodParam(int index, String p) {
    switch (index) {
      case 1:
        period1Param = p;
        break;
      case 2:
        period2Param = p;
        break;
      case 3:
        period3Param = p;
        break;
      case 4:
        period4Param = p;
        break;
      case 5:
        period5Param = p;
        break;
      default:
        throw new IndexOutOfBoundsException(INDEX_SHOULD_BE_IN_RANGE_FROM_1_TO_5);
    }
    return this;
  }

  public String getPeriodModeParameter(int periodIndex) {
    switch (periodIndex) {
      case 1:
        return period1Param;
      case 2:
        return period2Param;
      case 3:
        return period3Param;
      case 4:
        return period4Param;
      case 5:
        return period5Param;
      default:
        throw new IndexOutOfBoundsException(INDEX_SHOULD_BE_IN_RANGE_FROM_1_TO_5);
    }
  }

  public SnapshotDto setPeriodDate(int index, Long date) {
    switch (index) {
      case 1:
        period1Date = date;
        break;
      case 2:
        period2Date = date;
        break;
      case 3:
        period3Date = date;
        break;
      case 4:
        period4Date = date;
        break;
      case 5:
        period5Date = date;
        break;
      default:
        throw new IndexOutOfBoundsException(INDEX_SHOULD_BE_IN_RANGE_FROM_1_TO_5);
    }
    return this;
  }

  public Long getPeriodDate(int periodIndex) {
    switch (periodIndex) {
      case 1:
        return period1Date;
      case 2:
        return period2Date;
      case 3:
        return period3Date;
      case 4:
        return period4Date;
      case 5:
        return period5Date;
      default:
        throw new IndexOutOfBoundsException(INDEX_SHOULD_BE_IN_RANGE_FROM_1_TO_5);
    }
  }

  public Long getKey() {
    return id;
  }

  public SnapshotDto setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public Long getRootIdOrSelf() {
    return getRootId() == null ? getId() : getRootId();
  }
}
