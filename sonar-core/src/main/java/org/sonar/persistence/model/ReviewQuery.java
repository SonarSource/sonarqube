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

public final class ReviewQuery {
  private Boolean manualViolation;
  private Integer resourceId;
  private Integer userId;
  private Integer violationPermanentId;
  private Integer ruleId;
  private String status;
  private String resolution;

  private ReviewQuery() {
  }

  public static ReviewQuery create() {
    return new ReviewQuery();
  }

  public Boolean getManualViolation() {
    return manualViolation;
  }

  public ReviewQuery setManualViolation(Boolean manualViolation) {
    this.manualViolation = manualViolation;
    return this;
  }

  public Integer getResourceId() {
    return resourceId;
  }

  public ReviewQuery setResourceId(Integer resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public ReviewQuery setStatus(String status) {
    this.status = status;
    return this;
  }

  public Integer getUserId() {
    return userId;
  }

  public ReviewQuery setUserId(Integer userId) {
    this.userId = userId;
    return this;
  }

  public Integer getViolationPermanentId() {
    return violationPermanentId;
  }

  public ReviewQuery setViolationPermanentId(Integer violationPermanentId) {
    this.violationPermanentId = violationPermanentId;
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  public ReviewQuery setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  public String getResolution() {
    return resolution;
  }

  public ReviewQuery setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }
}
