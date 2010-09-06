/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.sonar.api.database.BaseIdentifiable;

import javax.persistence.*;

@Entity
@Table(name = "measure_data")
public class MeasureData extends BaseIdentifiable {

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "measure_id")
  private MeasureModel measure;

  @Column(name = "snapshot_id", updatable = true, nullable = true)
  private Integer snapshotId;

  @Column(name = "data", updatable = true, nullable = true, length = 167772150)
  private byte[] data;

  public MeasureData(MeasureModel measure) {
    this.measure = measure;
  }

  public MeasureData(MeasureModel measure, byte[] data) {
    this.measure = measure;
    this.data = data;
  }

  public MeasureData(MeasureModel measure, String dataString) {
    this.measure = measure;
    this.data = dataString.getBytes();
  }

  public MeasureData() {
  }

  public MeasureModel getMeasure() {
    return measure;
  }

  public void setMeasure(MeasureModel measure) {
    this.measure = measure;
  }

  public byte[] getData() {
    return data;
  }

  public String getText() {
    if (data != null) {
      return new String(data);
    }
    return null;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public Integer getSnapshotId() {
    return snapshotId;
  }

  public void setSnapshotId(Integer snapshotId) {
    this.snapshotId = snapshotId;
  }
}

