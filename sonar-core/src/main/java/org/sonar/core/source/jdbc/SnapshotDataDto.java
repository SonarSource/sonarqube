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

  private final long snapshotId;
  private final long resourceId;
  private final String data;
  private final String dataType;

  public SnapshotDataDto(long snapshotId, long resourceId, String data, String dataType) {
    this.snapshotId = snapshotId;
    this.resourceId = resourceId;
    this.data = data;
    this.dataType = dataType;
  }

  public long getSnapshotId() {
    return snapshotId;
  }

  public long getResourceId() {
    return resourceId;
  }

  public String getData() {
    return data;
  }

  public String getDataType() {
    return dataType;
  }
}
