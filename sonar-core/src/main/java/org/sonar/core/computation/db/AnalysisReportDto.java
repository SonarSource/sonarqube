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
import org.sonar.core.persistence.Dto;

import javax.annotation.Nullable;

public class AnalysisReportDto extends Dto<String> {

  private Long id;
  private String projectKey;
  private Status status;
  private String data;

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
    return String.format("analysis report {id:%s;project:'%s'}", getId(), getProjectKey());
  }

  public enum Status {
    PENDING, WORKING
  }
}
