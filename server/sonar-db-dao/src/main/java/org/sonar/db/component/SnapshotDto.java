/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import static org.apache.commons.lang.StringUtils.trimToNull;

public final class SnapshotDto {

  /**
   * This status is set on the snapshot at the beginning of the batch
   */
  public static final String STATUS_UNPROCESSED = "U";
  public static final String STATUS_PROCESSED = "P";
  public static final int MAX_VERSION_LENGTH = 100;
  public static final int MAX_BUILD_STRING_LENGTH = 100;

  private Long id;
  private String uuid;
  private String componentUuid;
  private Long createdAt;
  private Long buildDate;
  private String status = STATUS_UNPROCESSED;
  private Boolean last;
  // maps to "version" column in the table
  private String projectVersion;
  private String buildString;
  private String periodMode;
  private String periodParam;
  private Long periodDate;

  /**
   * SCM revision is provided by scanner and is optional.
   */
  @Nullable
  private String revision;

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

  public Boolean getLast() {
    return last;
  }

  public SnapshotDto setLast(Boolean last) {
    this.last = last;
    return this;
  }

  private static void checkLength(int maxLength, @Nullable String s, String label) {
    if (s != null) {
      checkArgument(s.length() <= maxLength,
        "%s length (%s) is longer than the maximum authorized (%s). '%s' was provided.", label, s.length(), maxLength, s);
    }
  }

  public SnapshotDto setProjectVersion(@Nullable String projectVersion) {
    checkLength(MAX_VERSION_LENGTH, projectVersion, "projectVersion");
    this.projectVersion = projectVersion;
    return this;
  }

  @CheckForNull
  public String getProjectVersion() {
    return projectVersion;
  }

  /**
   * Used by MyBatis
   */
  private void setRawProjectVersion(@Nullable String projectVersion) {
    this.projectVersion = trimToNull(projectVersion);
  }

  @CheckForNull
  public String getBuildString() {
    return buildString;
  }

  public SnapshotDto setBuildString(@Nullable String buildString) {
    checkLength(MAX_BUILD_STRING_LENGTH, buildString, "buildString");
    this.buildString = buildString;
    return this;
  }

  /**
   * Used by MyBatis
   */
  private void setRawBuildString(@Nullable String buildString) {
    this.buildString = trimToNull(buildString);
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

  @Nullable
  public String getRevision() {
    return revision;
  }

  public SnapshotDto setRevision(@Nullable String revision) {
    checkLength(100, revision, "revision");
    this.revision = revision;
    return this;
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
      Objects.equals(last, that.last) &&
      Objects.equals(projectVersion, that.projectVersion) &&
      Objects.equals(buildString, that.buildString) &&
      Objects.equals(periodMode, that.periodMode) &&
      Objects.equals(periodParam, that.periodParam) &&
      Objects.equals(periodDate, that.periodDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, uuid, componentUuid, createdAt, buildDate, status, last, projectVersion, buildString, periodMode, periodParam, periodDate);
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
      ", last=" + last +
      ", projectVersion='" + projectVersion + '\'' +
      ", buildString='" + buildString + '\'' +
      ", periodMode='" + periodMode + '\'' +
      ", periodParam='" + periodParam + '\'' +
      ", periodDate=" + periodDate +
      '}';
  }
}
