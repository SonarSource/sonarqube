/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.workflow;

import com.google.common.collect.ImmutableMap;
import org.sonar.api.workflow.Review;
import org.sonar.api.workflow.internal.DefaultReview;

import java.util.Map;

public final class ImmutableReview implements Review {
  private final Long violationId;
  private final Long reviewId;
  private final String ruleRepositoryKey;
  private final String ruleKey;
  private final String ruleName;
  private final Long line;
  private final boolean switchedOff;
  private final boolean manual;
  private final String message;
  private final String status;
  private final String resolution;
  private final String severity;
  private final Map<String, String> properties;

  /**
   * Warning : implementation is still mutable.
   */
  public ImmutableReview(DefaultReview review) {
    this.line = review.getLine();
    this.manual = review.isManual();
    this.message = review.getMessage();
    this.properties = ImmutableMap.copyOf(review.getProperties());
    this.resolution = review.getResolution();
    this.reviewId = review.getReviewId();
    this.ruleKey = review.getRuleKey();
    this.ruleRepositoryKey = review.getRuleRepositoryKey();
    this.ruleName = review.getRuleName();
    this.severity = review.getSeverity();
    this.status = review.getStatus();
    this.switchedOff = review.isSwitchedOff();
    this.violationId = review.getViolationId();
  }

  public Long getViolationId() {
    return violationId;
  }

  public Long getReviewId() {
    return reviewId;
  }

  public String getRuleName() {
    return ruleName;
  }

  public String getRuleRepositoryKey() {
    return ruleRepositoryKey;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public Long getLine() {
    return line;
  }

  public boolean isSwitchedOff() {
    return switchedOff;
  }

  public boolean isManual() {
    return manual;
  }

  public String getMessage() {
    return message;
  }

  public String getStatus() {
    return status;
  }

  public String getResolution() {
    return resolution;
  }

  public String getSeverity() {
    return severity;
  }

  public Map<String, String> getProperties() {
    return properties;
  }
}
