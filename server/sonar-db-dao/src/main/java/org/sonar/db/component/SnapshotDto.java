/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

public final class SnapshotDto {

  /**
   * This status is set on the snapshot at the beginning of the batch
   */
  public static final String STATUS_UNPROCESSED = "U";
  public static final String STATUS_PROCESSED = "P";
  public static final int MAX_VERSION_LENGTH = 100;

  private Long id;
  private String uuid;
  private String componentUuid;
  private Long createdAt;
  private Long buildDate;
  private String status = STATUS_UNPROCESSED;
  private Integer purgeStatus;
  private Boolean last;
  private String version;
  private String periodMode;
  private String periodParam;
  private Long periodDate;

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
    if (version != null) {
      checkArgument(version.length() <= MAX_VERSION_LENGTH,
        "Event name length (%s) is longer than the maximum authorized (%s). '%s' was provided.", version.length(), MAX_VERSION_LENGTH, version);
    }
    this.version = version;
    return this;
  }

  public SnapshotDto setPeriodMode(@Nullable String p) {
    periodMode = p;
    return this;
  }

  @CheckForNull
  public String getPeriodMode() {
    return periodMode;
  }

  public SnapshotDto setPeriodParam(@Nullable String p) {
    periodParam = p;
    return this;
  }

  @CheckForNull
  public String getPeriodModeParameter() {
    return periodParam;
  }

  public SnapshotDto setPeriodDate(@Nullable Long date) {
    periodDate = date;
    return this;
  }

  @CheckForNull
  public Long getPeriodDate() {
    return periodDate;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SnapshotDto that = (SnapshotDto) o;
    return Objects.equals(id, that.id) &&
      Objects.equals(uuid, that.uuid) &&
      Objects.equals(componentUuid, that.componentUuid) &&
      Objects.equals(createdAt, that.createdAt) &&
      Objects.equals(buildDate, that.buildDate) &&
      Objects.equals(status, that.status) &&
      Objects.equals(purgeStatus, that.purgeStatus) &&
      Objects.equals(last, that.last) &&
      Objects.equals(version, that.version) &&
      Objects.equals(periodMode, that.periodMode) &&
      Objects.equals(periodParam, that.periodParam) &&
      Objects.equals(periodDate, that.periodDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, uuid, componentUuid, createdAt, buildDate, status, purgeStatus, last, version, periodMode, periodParam, periodDate);
  }

  @Override
  public String toString() {
    return "SnapshotDto{" +
      "id=" + id +
      ", uuid='" + uuid + '\'' +
      ", componentUuid='" + componentUuid + '\'' +
      ", createdAt=" + createdAt +
      ", buildDate=" + buildDate +
      ", status='" + status + '\'' +
      ", purgeStatus=" + purgeStatus +
      ", last=" + last +
      ", version='" + version + '\'' +
      ", periodMode='" + periodMode + '\'' +
      ", periodParam='" + periodParam + '\'' +
      ", periodDate=" + periodDate +
      '}';
  }
}
