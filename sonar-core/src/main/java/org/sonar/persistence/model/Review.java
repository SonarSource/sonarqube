/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.persistence.model;

import java.util.Date;

/**
 * @since 2.13
 */
public class Review {
  private Long id;
  private Integer userId;
  private Integer assigneeId;
  private String title;
  private String status;
  private String resolution;
  private Integer violationPermanentId;
  private Integer projectId;
  private Integer resourceId;
  private Integer line;
  private Date createdAt;
  private Date updatedAt;
  private String severity;
  private Integer ruleId;
  private Boolean manualViolation;

  public Long getId() {
    return id;
  }

  public Review setId(Long id) {
    this.id = id;
    return this;
  }

  public Integer getUserId() {
    return userId;
  }

  public Review setUserId(Integer userId) {
    this.userId = userId;
    return this;
  }

  public Integer getAssigneeId() {
    return assigneeId;
  }

  public void setAssigneeId(Integer assigneeId) {
    this.assigneeId = assigneeId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getResolution() {
    return resolution;
  }

  public void setResolution(String resolution) {
    this.resolution = resolution;
  }

  public Integer getViolationPermanentId() {
    return violationPermanentId;
  }

  public void setViolationPermanentId(Integer violationPermanentId) {
    this.violationPermanentId = violationPermanentId;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public Integer getResourceId() {
    return resourceId;
  }

  public void setResourceId(Integer resourceId) {
    this.resourceId = resourceId;
  }

  public Integer getLine() {
    return line;
  }

  public void setLine(Integer line) {
    this.line = line;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  public void setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
  }

  public Boolean getManualViolation() {
    return manualViolation;
  }

  public void setManualViolation(Boolean manualViolation) {
    this.manualViolation = manualViolation;
  }
}
