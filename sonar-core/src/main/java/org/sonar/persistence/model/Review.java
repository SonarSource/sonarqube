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

  public static final String STATUS_OPEN = "OPEN";
  public static final String STATUS_CLOSED = "CLOSED";

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

  public Review setAssigneeId(Integer assigneeId) {
    this.assigneeId = assigneeId;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public Review setTitle(String title) {
    this.title = title;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public Review setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getResolution() {
    return resolution;
  }

  public Review setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  public Integer getViolationPermanentId() {
    return violationPermanentId;
  }

  public Review setViolationPermanentId(Integer violationPermanentId) {
    this.violationPermanentId = violationPermanentId;
    return this;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public Review setProjectId(Integer projectId) {
    this.projectId = projectId;
    return this;
  }

  public Integer getResourceId() {
    return resourceId;
  }

  public Review setResourceId(Integer resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public Integer getLine() {
    return line;
  }

  public Review setLine(Integer line) {
    this.line = line;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public Review setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public Review setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getSeverity() {
    return severity;
  }

  public Review setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  public Review setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  public Boolean getManualViolation() {
    return manualViolation;
  }

  public Review setManualViolation(Boolean b) {
    this.manualViolation = b;
    return this;
  }
}
