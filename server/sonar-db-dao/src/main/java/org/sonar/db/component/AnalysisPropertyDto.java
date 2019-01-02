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

import static java.util.Objects.requireNonNull;

public class AnalysisPropertyDto {

  private String uuid;
  private String snapshotUuid;
  private String key;
  private String value;
  private Long createdAt;


  public String getUuid() {
    return uuid;
  }

  public AnalysisPropertyDto setUuid(String uuid) {
    requireNonNull(uuid, "uuid cannot be null");
    this.uuid = uuid;
    return this;
  }

  public String getSnapshotUuid() {
    return snapshotUuid;
  }

  public AnalysisPropertyDto setSnapshotUuid(String snapshotUuid) {
    requireNonNull(snapshotUuid, "snapshotUuid cannot be null");
    this.snapshotUuid = snapshotUuid;
    return this;
  }

  public String getKey() {
    return key;
  }

  public AnalysisPropertyDto setKey(String key) {
    requireNonNull(key, "key cannot be null");
    this.key = key;
    return this;
  }

  public String getValue() {
    return value;
  }

  public AnalysisPropertyDto setValue(String value) {
    requireNonNull(value, "value cannot be null");
    this.value = value;
    return this;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public AnalysisPropertyDto setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @Override
  public String toString() {
    return "BranchDto{" + "uuid='" + uuid + '\'' +
      ", snapshotUuid='" + snapshotUuid + '\'' +
      ", key='" + key + '\'' +
      ", value='" + value + "'" +
      ", createdAt=" + createdAt +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AnalysisPropertyDto)) {
      return false;
    }
    AnalysisPropertyDto that = (AnalysisPropertyDto) o;
    return Objects.equals(uuid, that.uuid) &&
      Objects.equals(snapshotUuid, that.snapshotUuid) &&
      Objects.equals(key, that.key) &&
      Objects.equals(value, that.value) &&
      Objects.equals(createdAt, that.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, snapshotUuid, key, value, createdAt);
  }
}
