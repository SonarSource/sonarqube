/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.core.source.jdbc;

/**
 * @since 3.6
 */
public class SnapshotDataDto {

  private long id;
  private long snapshotId;
  private long resourceId;
  private String data;
  private String dataType;

  public long getId() {
    return id;
  }

  public SnapshotDataDto setId(long id) {
    this.id = id;
    return this;
  }

  public long getSnapshotId() {
    return snapshotId;
  }

  public SnapshotDataDto setSnapshotId(long snapshotId) {
    this.snapshotId = snapshotId;
    return this;
  }

  public long getResourceId() {
    return resourceId;
  }

  public SnapshotDataDto setResourceId(long resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public String getData() {
    return data;
  }

  public SnapshotDataDto setData(String data) {
    this.data = data;
    return this;
  }

  public String getDataType() {
    return dataType;
  }

  public SnapshotDataDto setDataType(String dataType) {
    this.dataType = dataType;
    return this;
  }
}
