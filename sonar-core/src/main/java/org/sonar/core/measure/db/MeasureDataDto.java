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

package org.sonar.core.measure.db;

import com.google.common.base.Charsets;

import javax.annotation.CheckForNull;

public class MeasureDataDto {

  private Integer id;

  private Long measureId;

  private Integer snapshotId;

  private byte[] data;

  public Integer getId() {
    return id;
  }

  public MeasureDataDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public Long getMeasureId() {
    return measureId;
  }

  public MeasureDataDto setMeasureId(Long measureId) {
    this.measureId = measureId;
    return this;
  }

  public Integer getSnapshotId() {
    return snapshotId;
  }

  public MeasureDataDto setSnapshotId(Integer snapshotId) {
    this.snapshotId = snapshotId;
    return this;
  }

  public byte[] getData() {
    return data;
  }

  public MeasureDataDto setData(byte[] data) {
    this.data = data;
    return this;
  }

  @CheckForNull
  public String getText() {
    if (data != null) {
      return new String(data, Charsets.UTF_8);
    }
    return null;
  }
}
