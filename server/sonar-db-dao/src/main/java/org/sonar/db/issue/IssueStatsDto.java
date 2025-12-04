/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.issue;

import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.issue.IssueStatus;

public final class IssueStatsDto {
  private String resolution;
  private String severity;
  private String mqrSeverity;
  private String status;
  private Integer issueType;
  private String repositoryKey;
  private String ruleKey;

  public IssueStatsDto() {
    // empty constructor
  }

  public String getResolution() {
    return resolution;
  }

  public IssueStatsDto setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  public String getSeverity() {
    return severity;
  }

  public IssueStatsDto setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  public String getMqrSeverity() {
    return mqrSeverity;
  }

  public IssueStatsDto setMqrSeverity(String mqrSeverity) {
    this.mqrSeverity = mqrSeverity;
    return this;
  }

  public String getRepositoryKey() {
    return repositoryKey;
  }

  public void setRepositoryKey(String repositoryKey) {
    this.repositoryKey = repositoryKey;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public void setRuleKey(String ruleKey) {
    this.ruleKey = ruleKey;
  }

  public String getStatus() {
    return status;
  }

  public IssueStatsDto setStatus(String status) {
    this.status = status;
    return this;
  }

  @CheckForNull
  public String getIssueStatus() {
    return Optional.ofNullable(IssueStatus.of(status, resolution)).map(IssueStatus::name).orElse(null);
  }

  public Integer getIssueType() {
    return issueType;
  }

  public IssueStatsDto setIssueType(Integer issueType) {
    this.issueType = issueType;
    return this;
  }
}
