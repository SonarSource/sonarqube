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

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public void setSnapshotId(long snapshotId) {
    this.snapshotId = snapshotId;
  }

  public void setResourceId(long resourceId) {
    this.resourceId = resourceId;
  }

  public void setData(String data) {
    this.data = data;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SnapshotDataDto that = (SnapshotDataDto) o;

    if (id != that.id) return false;
    if (resourceId != that.resourceId) return false;
    if (snapshotId != that.snapshotId) return false;
    if (data != null ? !data.equals(that.data) : that.data != null) return false;
    if (dataType != null ? !dataType.equals(that.dataType) : that.dataType != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (int) (snapshotId ^ (snapshotId >>> 32));
    result = 31 * result + (int) (resourceId ^ (resourceId >>> 32));
    result = 31 * result + (data != null ? data.hashCode() : 0);
    result = 31 * result + (dataType != null ? dataType.hashCode() : 0);
    return result;
  }
}
