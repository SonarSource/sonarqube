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
package org.sonar.core.resource;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;

public final class SnapshotDto {
  private Long id;
  private Long parentId;
  private Long rootId;

  private Date date;
  private Date buildDate;
  private Long resourceId;
  private String status;
  private Integer purgeStatus;
  private Boolean last;
  private String scope;
  private String qualifier;
  private String version;
  private String path;
  private Integer depth;
  private Long rootProjectId;

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

  private Date period1Date;
  private Date period2Date;
  private Date period3Date;
  private Date period4Date;
  private Date period5Date;

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

  public Date getDate() {
    return date;
  }

  public SnapshotDto setDate(Date date) {
    this.date = date;
    return this;
  }

  public Date getBuildDate() {
    return buildDate;
  }

  public SnapshotDto setBuildDate(Date buildDate) {
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

  @CheckForNull
  public String getPeriod1Mode() {
    return period1Mode;
  }

  public SnapshotDto setPeriod1Mode(@Nullable String period1Mode) {
    this.period1Mode = period1Mode;
    return this;
  }

  @CheckForNull
  public String getPeriod2Mode() {
    return period2Mode;
  }

  public SnapshotDto setPeriod2Mode(@Nullable String period2Mode) {
    this.period2Mode = period2Mode;
    return this;
  }

  @CheckForNull
  public String getPeriod3Mode() {
    return period3Mode;
  }

  public SnapshotDto setPeriod3Mode(@Nullable String period3Mode) {
    this.period3Mode = period3Mode;
    return this;
  }

  @CheckForNull
  public String getPeriod4Mode() {
    return period4Mode;
  }

  public SnapshotDto setPeriod4Mode(@Nullable String period4Mode) {
    this.period4Mode = period4Mode;
    return this;
  }

  @CheckForNull
  public String getPeriod5Mode() {
    return period5Mode;
  }

  public SnapshotDto setPeriod5Mode(@Nullable String period5Mode) {
    this.period5Mode = period5Mode;
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
        throw new IndexOutOfBoundsException("Index of periodMode is between 1 and 5");
    }
  }

  @CheckForNull
  public String getPeriod1Param() {
    return period1Param;
  }

  public SnapshotDto setPeriod1Param(@Nullable String period1Param) {
    this.period1Param = period1Param;
    return this;
  }

  @CheckForNull
  public String getPeriod2Param() {
    return period2Param;
  }

  public SnapshotDto setPeriod2Param(@Nullable String period2Param) {
    this.period2Param = period2Param;
    return this;
  }

  @CheckForNull
  public String getPeriod3Param() {
    return period3Param;
  }

  public SnapshotDto setPeriod3Param(@Nullable String period3Param) {
    this.period3Param = period3Param;
    return this;
  }

  @CheckForNull
  public String getPeriod4Param() {
    return period4Param;
  }

  public SnapshotDto setPeriod4Param(@Nullable String period4Param) {
    this.period4Param = period4Param;
    return this;
  }

  @CheckForNull
  public String getPeriod5Param() {
    return period5Param;
  }

  public SnapshotDto setPeriod5Param(@Nullable String period5Param) {
    this.period5Param = period5Param;
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
        throw new IndexOutOfBoundsException("Index of periodModeParameter is between 1 and 5");
    }
  }

  @CheckForNull
  public Date getPeriod1Date() {
    return period1Date;
  }

  public SnapshotDto setPeriod1Date(@Nullable Date period1Date) {
    this.period1Date = period1Date;
    return this;
  }

  @CheckForNull
  public Date getPeriod2Date() {
    return period2Date;
  }

  public SnapshotDto setPeriod2Date(@Nullable Date period2Date) {
    this.period2Date = period2Date;
    return this;
  }

  @CheckForNull
  public Date getPeriod3Date() {
    return period3Date;
  }

  public SnapshotDto setPeriod3Date(@Nullable Date period3Date) {
    this.period3Date = period3Date;
    return this;
  }

  @CheckForNull
  public Date getPeriod4Date() {
    return period4Date;
  }

  public SnapshotDto setPeriod4Date(@Nullable Date period4Date) {
    this.period4Date = period4Date;
    return this;
  }

  @CheckForNull
  public Date getPeriod5Date() {
    return period5Date;
  }

  public SnapshotDto setPeriod5Date(@Nullable Date period5Date) {
    this.period5Date = period5Date;
    return this;
  }

  public Date getPeriodDate(int periodIndex) {
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
        throw new IndexOutOfBoundsException("Index of periodDate is between 1 and 5");
    }
  }
}
