/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.agent;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Maps a row of the {@code AGENT_JOBS} table. Does not carry any {@code REMEDIATION_AGENT_JOBS}
 * columns.
 */
public class AgentJobDto {

  private String id;
  private String projectId;
  private String projectKey;
  private String projectName;
  private String branch;
  private String repositoryUrl;
  private String revision;
  private String agentType;
  private String workflowType;
  private String analysisType;
  private String status;
  private String subStatus;
  private String errorKey;
  private Integer findingsCount;
  private long createdAt;
  private long updatedAt;
  private Long startedAt;
  private Long finishedAt;

  public AgentJobDto() {
    // no-op: fields are populated via the fluent setters below (by MyBatis when mapping a row, or by callers)
  }

  public String getId() {
    return id;
  }

  public AgentJobDto setId(String id) {
    this.id = id;
    return this;
  }

  public String getProjectId() {
    return projectId;
  }

  public AgentJobDto setProjectId(String projectId) {
    this.projectId = projectId;
    return this;
  }

  @CheckForNull
  public String getProjectKey() {
    return projectKey;
  }

  public AgentJobDto setProjectKey(@Nullable String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  @CheckForNull
  public String getProjectName() {
    return projectName;
  }

  public AgentJobDto setProjectName(@Nullable String projectName) {
    this.projectName = projectName;
    return this;
  }

  @CheckForNull
  public String getBranch() {
    return branch;
  }

  public AgentJobDto setBranch(@Nullable String branch) {
    this.branch = branch;
    return this;
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  public AgentJobDto setRepositoryUrl(String repositoryUrl) {
    this.repositoryUrl = repositoryUrl;
    return this;
  }

  @CheckForNull
  public String getRevision() {
    return revision;
  }

  public AgentJobDto setRevision(@Nullable String revision) {
    this.revision = revision;
    return this;
  }

  public String getAgentType() {
    return agentType;
  }

  public AgentJobDto setAgentType(String agentType) {
    this.agentType = agentType;
    return this;
  }

  @CheckForNull
  public String getWorkflowType() {
    return workflowType;
  }

  public AgentJobDto setWorkflowType(@Nullable String workflowType) {
    this.workflowType = workflowType;
    return this;
  }

  public String getAnalysisType() {
    return analysisType;
  }

  public AgentJobDto setAnalysisType(String analysisType) {
    this.analysisType = analysisType;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public AgentJobDto setStatus(String status) {
    this.status = status;
    return this;
  }

  @CheckForNull
  public String getSubStatus() {
    return subStatus;
  }

  public AgentJobDto setSubStatus(@Nullable String subStatus) {
    this.subStatus = subStatus;
    return this;
  }

  @CheckForNull
  public String getErrorKey() {
    return errorKey;
  }

  public AgentJobDto setErrorKey(@Nullable String errorKey) {
    this.errorKey = errorKey;
    return this;
  }

  @CheckForNull
  public Integer getFindingsCount() {
    return findingsCount;
  }

  public AgentJobDto setFindingsCount(@Nullable Integer findingsCount) {
    this.findingsCount = findingsCount;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public AgentJobDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public AgentJobDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  @CheckForNull
  public Long getStartedAt() {
    return startedAt;
  }

  public AgentJobDto setStartedAt(@Nullable Long startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  @CheckForNull
  public Long getFinishedAt() {
    return finishedAt;
  }

  public AgentJobDto setFinishedAt(@Nullable Long finishedAt) {
    this.finishedAt = finishedAt;
    return this;
  }
}
