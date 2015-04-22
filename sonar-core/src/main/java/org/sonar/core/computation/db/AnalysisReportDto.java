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

import javax.annotation.CheckForNull;

public class AnalysisReportDto {
  private Long id;
  private String projectKey;
  private Status status;
  private String uuid;
  private Long createdAt;
  private Long updatedAt;
  private Long startedAt;
  private Long finishedAt;

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

  public String getUuid() {
    return uuid;
  }

  public AnalysisReportDto setUuid(String s) {
    this.uuid = s;
    return this;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("id", getId())
      .add("projectKey", getProjectKey())
      .add("uuid", getUuid())
      .add("status", getStatus())
      .add("createdAt", getCreatedAt())
      .add("startedAt", getStartedAt())
      .add("finishedAt", getFinishedAt())
      .toString();
  }

  @CheckForNull
  public Long getStartedAt() {
    return startedAt;
  }

  public AnalysisReportDto setStartedAt(Long startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  @CheckForNull
  public Long getFinishedAt() {
    return finishedAt;
  }

  public AnalysisReportDto setFinishedAt(Long finishedAt) {
    this.finishedAt = finishedAt;
    return this;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public AnalysisReportDto setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Long getUpdatedAt() {
    return updatedAt;
  }

  public AnalysisReportDto setUpdatedAt(Long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public enum Status {
    PENDING, WORKING, SUCCESS, FAILED, CANCELLED
  }
}
