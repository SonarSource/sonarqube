/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public final class SnapshotDto {

  /**
   * This status is set on the snapshot at the beginning of the batch
   */
  public static final String STATUS_UNPROCESSED = "U";
  public static final String STATUS_PROCESSED = "P";

  private static final String INDEX_SHOULD_BE_IN_RANGE_FROM_1_TO_5 = "Index should be in range from 1 to 5";

  private Long id;
  private String uuid;
  private String componentUuid;
  private Long createdAt;
  private Long buildDate;
  private String status = STATUS_UNPROCESSED;
  private Integer purgeStatus;
  private Boolean last;
  private String version;

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

  public SnapshotDto setUuid(String s) {
    this.uuid = s;
    return this;
  }

  public String getUuid() {
    return this.uuid;
  }

  public Long getBuildDate() {
    return buildDate;
  }

  public SnapshotDto setBuildDate(Long buildDate) {
    this.buildDate = buildDate;
    return this;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public SnapshotDto setComponentUuid(String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public SnapshotDto setStatus(String status) {
    this.status = status;
    return this;
  }

  @CheckForNull
  public Integer getPurgeStatus() {
    return purgeStatus;
  }

  public SnapshotDto setPurgeStatus(@Nullable Integer purgeStatus) {
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

  /**
   * Version is only available on projects and modules
   */
  @CheckForNull
  public String getVersion() {
    return version;
  }

  public SnapshotDto setVersion(@Nullable String version) {
    this.version = version;
    return this;
  }

  public SnapshotDto setPeriodMode(int index, @Nullable String p) {
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

  @CheckForNull
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

  public SnapshotDto setPeriodParam(int index, @Nullable String p) {
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

  @CheckForNull
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

  public SnapshotDto setPeriodDate(int index, @Nullable Long date) {
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

  @CheckForNull
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

  public SnapshotDto setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * @return analysis date
   */
  public Long getCreatedAt() {
    return createdAt;
  }
}
