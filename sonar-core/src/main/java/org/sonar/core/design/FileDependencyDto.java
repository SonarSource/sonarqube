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

package org.sonar.core.design;

public final class FileDependencyDto {

  private Long id;
  private String fromComponentUuid;
  private String fromParentUuid;
  private String toComponentUuid;
  private String toParentUuid;
  private Long rootProjectSnapshotId;
  private Integer weight;
  private Long createdAt;

  public Long getId() {
    return id;
  }

  public FileDependencyDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getFromComponentUuid() {
    return fromComponentUuid;
  }

  public FileDependencyDto setFromComponentUuid(String fromComponentUuid) {
    this.fromComponentUuid = fromComponentUuid;
    return this;
  }

  public String getFromParentUuid() {
    return fromParentUuid;
  }

  public FileDependencyDto setFromParentUuid(String fromParentUuid) {
    this.fromParentUuid = fromParentUuid;
    return this;
  }

  public Long getRootProjectSnapshotId() {
    return rootProjectSnapshotId;
  }

  public FileDependencyDto setRootProjectSnapshotId(Long rootProjectSnapshotId) {
    this.rootProjectSnapshotId = rootProjectSnapshotId;
    return this;
  }

  public String getToComponentUuid() {
    return toComponentUuid;
  }

  public FileDependencyDto setToComponentUuid(String toComponentUuid) {
    this.toComponentUuid = toComponentUuid;
    return this;
  }

  public String getToParentUuid() {
    return toParentUuid;
  }

  public FileDependencyDto setToParentUuid(String toParentUuid) {
    this.toParentUuid = toParentUuid;
    return this;
  }

  public Integer getWeight() {
    return weight;
  }

  public FileDependencyDto setWeight(Integer weight) {
    this.weight = weight;
    return this;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public FileDependencyDto setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
