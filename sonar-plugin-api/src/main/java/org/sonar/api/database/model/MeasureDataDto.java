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
package org.sonar.api.database.model;

public class MeasureDataDto {
  private final Long measureId;
  private final Integer snapshotId;
  private final byte[] data;

  public MeasureDataDto(Long measureId, Integer snapshotId, byte[] data) {
    this.measureId = measureId;
    this.snapshotId = snapshotId;
    this.data = data;
  }

  public Long getMeasureId() {
    return measureId;
  }

  public Integer getSnapshotId() {
    return snapshotId;
  }

  public byte[] getData() {
    return data;
  }
}
