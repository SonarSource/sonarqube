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
package org.sonar.core.computation.db;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import org.sonar.core.persistence.Dto;

import javax.annotation.Nullable;

import java.util.Date;

public class AnalysisReportDto extends Dto<String> {

  private Long id;
  private String projectKey;
  private Status status;
  private String data;
  private Long snapshotId;
  private Date startedAt;
  private Date finishedAt;

  public AnalysisReportDto() {
    super();
  }

  @VisibleForTesting
  public static AnalysisReportDto newForTests(Long id) {
    AnalysisReportDto report = new AnalysisReportDto();
    report.id = id;

    return report;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public AnalysisReportDto setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public Status getStatus() {
    return status;
  }

  public AnalysisReportDto setStatus(Status status) {
    this.status = status;
    return this;
  }

  public void fail() {
    this.status = Status.FAILED;
  }

  public void succeed() {
    this.status = Status.SUCCESS;
  }

  public String getData() {
    return data;
  }

  public AnalysisReportDto setData(@Nullable String data) {
    this.data = data;
    return this;
  }

  @Override
  public String getKey() {
    return String.valueOf(getId());
  }

  public Long getId() {
    return id;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("id", getId())
      .add("projectKey", getProjectKey())
      .add("snapshotId", getSnapshotId())
      .add("status", getStatus())
      .add("createdAt", getCreatedAt())
      .add("startedAt", getStartedAt())
      .add("finishedAt", getFinishedAt())
      .toString();
  }

  public Long getSnapshotId() {
    return snapshotId;
  }

  public AnalysisReportDto setSnapshotId(Long snapshotId) {
    this.snapshotId = snapshotId;
    return this;
  }

  public Date getStartedAt() {
    return startedAt;
  }

  public AnalysisReportDto setStartedAt(Date startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  public Date getFinishedAt() {
    return finishedAt;
  }

  public AnalysisReportDto setFinishedAt(Date finishedAt) {
    this.finishedAt = finishedAt;
    return this;
  }

  @Override
  public AnalysisReportDto setUpdatedAt(Date datetime) {
    super.setUpdatedAt(datetime);
    return this;
  }

  @Override
  public AnalysisReportDto setCreatedAt(Date datetime) {
    super.setCreatedAt(datetime);
    return this;
  }

  public enum Status {
    PENDING, WORKING, SUCCESS, FAILED;

    public boolean isInFinalState() {
      return SUCCESS.equals(this) || FAILED.equals(this);
    }
  }
}
