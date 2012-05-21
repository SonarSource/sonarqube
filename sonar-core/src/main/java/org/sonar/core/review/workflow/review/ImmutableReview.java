/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.core.review.workflow.review;

import java.util.Map;

public class ImmutableReview implements Review {
  private Long violationId;
  private Long reviewId;
  private Long ruleId;
  private Long assigneeId;
  private Long line;
  private boolean switchedOff = false;
  private boolean manual = false;
  private String message;
  private String status;
  private String resolution;
  private String severity;
  private Map<String, String> properties;

  public Long getViolationId() {
    return violationId;
  }

  void setViolationId(Long violationId) {
    this.violationId = violationId;
  }

  public Long getReviewId() {
    return reviewId;
  }

  void setReviewId(Long reviewId) {
    this.reviewId = reviewId;
  }

  public Long getRuleId() {
    return ruleId;
  }

  void setRuleId(Long ruleId) {
    this.ruleId = ruleId;
  }

  public Long getAssigneeId() {
    return assigneeId;
  }

  void setAssigneeId(Long assigneeId) {
    this.assigneeId = assigneeId;
  }

  public Long getLine() {
    return line;
  }

  void setLine(Long line) {
    this.line = line;
  }

  public boolean isSwitchedOff() {
    return switchedOff;
  }

  void setSwitchedOff(boolean switchedOff) {
    this.switchedOff = switchedOff;
  }

  public boolean isManual() {
    return manual;
  }

  void setManual(boolean manual) {
    this.manual = manual;
  }

  public String getMessage() {
    return message;
  }

  void setMessage(String message) {
    this.message = message;
  }

  public String getStatus() {
    return status;
  }

  void setStatus(String status) {
    this.status = status;
  }

  public String getResolution() {
    return resolution;
  }

  void setResolution(String resolution) {
    this.resolution = resolution;
  }

  public String getSeverity() {
    return severity;
  }

  void setSeverity(String severity) {
    this.severity = severity;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
}
