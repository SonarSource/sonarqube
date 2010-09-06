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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.database.BaseIdentifiable;

import java.util.Date;
import javax.persistence.*;

/**
 * Class to map an aysync measure with hibernate model
 */
@Entity
@Table(name = "async_measure_snapshots")
public class AsyncMeasureSnapshot extends BaseIdentifiable {

  @Column(name = "project_measure_id", updatable = true, nullable = true)
  private Long measureId;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "measure_date", updatable = true, nullable = true)
  private Date measureDate;

  @Column(name = "snapshot_id", updatable = true, nullable = true)
  private Integer snapshotId;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "snapshot_date", updatable = true, nullable = true)
  private Date snapshotDate;

  @Column(name = "metric_id", updatable = true, nullable = true)
  private Integer metricId;

  @Column(name = "project_id", updatable = true, nullable = true)
  private Integer projectId;

  /**
   * This is the constructor to use
   *
   * @param measureId
   * @param snapshotId the snapshot id to which the measure is attached
   * @param measureDate the date of the measure
   * @param snapshotDate the snapshot date
   * @param metricId the metric the measure is attached to
   * @param projectId the id of the project
   */
  public AsyncMeasureSnapshot(Long measureId, Integer snapshotId, Date measureDate, Date snapshotDate, Integer metricId, Integer projectId) {
    this.measureId = measureId;
    this.measureDate = measureDate;
    this.snapshotId = snapshotId;
    this.snapshotDate = snapshotDate;
    this.projectId = projectId;
    this.metricId = metricId;
  }

  /**
   * Default constructor
   */
  public AsyncMeasureSnapshot() {
  }

  public Long getMeasureId() {
    return measureId;
  }

  public void setMeasureId(Long measureId) {
    this.measureId = measureId;
  }

  public Integer getSnapshotId() {
    return snapshotId;
  }

  public void setSnapshotId(Integer snapshotId) {
    this.snapshotId = snapshotId;
  }

  public Date getMeasureDate() {
    return measureDate;
  }

  public void setMeasureDate(Date measureDate) {
    this.measureDate = measureDate;
  }

  public Date getSnapshotDate() {
    return snapshotDate;
  }

  public void setSnapshotDate(Date snapshotDate) {
    this.snapshotDate = snapshotDate;
  }

  public Integer getMetricId() {
    return metricId;
  }

  public void setMetricId(Integer metricId) {
    this.metricId = metricId;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public void setMeasure(MeasureModel measure) {
    setMeasureId(measure.getId());
    setMeasureDate(measure.getMeasureDate());
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof AsyncMeasureSnapshot)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    AsyncMeasureSnapshot other = (AsyncMeasureSnapshot) obj;
    return new EqualsBuilder()
      .append(measureId, other.getMeasureId())
      .append(measureDate, other.getMeasureDate())
      .append(snapshotId, other.getSnapshotId())
      .append(snapshotDate, other.getSnapshotDate())
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(measureId)
      .append(measureDate)
      .append(snapshotDate)
      .append(snapshotId)
      .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("id", getId())
      .append("measureId", measureId)
      .append("measureDate", measureDate)
      .append("snapshotId", snapshotId)
      .append("snapshotDate", snapshotDate)
      .toString();
  }
}
