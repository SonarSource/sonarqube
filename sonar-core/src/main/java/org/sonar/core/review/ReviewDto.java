/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.review;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * @since 2.13
 */
public final class ReviewDto {

  public static final String STATUS_OPEN = "OPEN";
  public static final String STATUS_REOPENED = "REOPENED";
  public static final String STATUS_RESOLVED = "RESOLVED";
  public static final String STATUS_CLOSED = "CLOSED";

  public static final String RESOLUTION_FALSE_POSITIVE = "FALSE-POSITIVE";
  public static final String RESOLUTION_FIXED = "FIXED";

  private Long id;
  private Integer userId;
  private Long assigneeId;
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
  private boolean manualViolation;
  private boolean manualSeverity;
  private Integer actionPlanId;
  private String data;

  public Long getId() {
    return id;
  }

  public ReviewDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Integer getUserId() {
    return userId;
  }

  public ReviewDto setUserId(Integer userId) {
    this.userId = userId;
    return this;
  }

  public Long getAssigneeId() {
    return assigneeId;
  }

  public ReviewDto setAssigneeId(@Nullable Long assigneeId) {
    this.assigneeId = assigneeId;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public ReviewDto setTitle(String title) {
    this.title = title;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public ReviewDto setStatus(@Nullable String status) {
    this.status = status;
    return this;
  }

  public String getResolution() {
    return resolution;
  }

  public ReviewDto setResolution(@Nullable String resolution) {
    this.resolution = resolution;
    return this;
  }

  public Integer getViolationPermanentId() {
    return violationPermanentId;
  }

  public ReviewDto setViolationPermanentId(Integer violationPermanentId) {
    this.violationPermanentId = violationPermanentId;
    return this;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public ReviewDto setProjectId(Integer projectId) {
    this.projectId = projectId;
    return this;
  }

  public Integer getResourceId() {
    return resourceId;
  }

  public ReviewDto setResourceId(Integer resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public Integer getLine() {
    return line;
  }

  public ReviewDto setLine(@Nullable Integer line) {
    this.line = line;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public ReviewDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public ReviewDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getSeverity() {
    return severity;
  }

  public ReviewDto setSeverity(@Nullable String severity) {
    this.severity = severity;
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  public ReviewDto setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  public boolean getManualViolation() {
    return manualViolation;
  }

  public boolean isManualViolation() {
    return manualViolation;
  }

  public ReviewDto setManualViolation(boolean b) {
    this.manualViolation = b;
    return this;
  }

  public boolean getManualSeverity() {
    return manualSeverity;
  }

  public ReviewDto setManualSeverity(boolean b) {
    this.manualSeverity = b;
    return this;
  }

  public boolean isManualSeverity() {
    return manualSeverity;
  }

  public Integer getActionPlanId() {
    return actionPlanId;
  }

  public ReviewDto setActionPlanId(@Nullable Integer i) {
    this.actionPlanId = i;
    return this;
  }

  public String getData() {
    return data;
  }

  public ReviewDto setData(String s) {
    Preconditions.checkArgument(s == null || s.length() <= 4000,
        "Review data must not exceed 4000 characters: " + s);
    this.data = s;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ReviewDto reviewDto = (ReviewDto) o;
    return !(id != null ? !id.equals(reviewDto.id) : reviewDto.id != null);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
