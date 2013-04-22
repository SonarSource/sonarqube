/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.dependency;

public final class DependencyDto {
  private Long id;
  private Long fromSnapshotId;
  private Long toSnapshotId;
  private String usage;

  public Long getId() {
    return id;
  }

  public DependencyDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getFromSnapshotId() {
    return fromSnapshotId;
  }

  public DependencyDto setFromSnapshotId(Long fromSnapshotId) {
    this.fromSnapshotId = fromSnapshotId;
    return this;
  }

  public Long getToSnapshotId() {
    return toSnapshotId;
  }

  public DependencyDto setToSnapshotId(Long toSnapshotId) {
    this.toSnapshotId = toSnapshotId;
    return this;
  }

  public String getUsage() {
    return usage;
  }

  public DependencyDto setUsage(String usage) {
    this.usage = usage;
    return this;
  }
}
