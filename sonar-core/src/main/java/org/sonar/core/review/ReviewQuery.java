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
package org.sonar.core.review;

import java.util.Collection;
import java.util.List;

import org.sonar.core.persistence.DatabaseUtils;

import com.google.common.collect.Lists;

/**
 * @since 2.13
 */
public final class ReviewQuery {
  private Boolean manualViolation;
  private Boolean manualSeverity;
  private Integer resourceId;
  private Integer userId;
  private List<Integer> violationPermanentIds;
  private Integer ruleId;
  private List<String> statuses;
  private List<String> resolutions;
  private Boolean noAssignee;
  private Boolean planned;

  private ReviewQuery() {
  }

  private ReviewQuery(ReviewQuery other) {
    this.manualViolation = other.manualViolation;
    this.manualSeverity = other.manualSeverity;
    this.resourceId = other.resourceId;
    this.userId = other.userId;
    this.violationPermanentIds = other.violationPermanentIds;
    this.ruleId = other.ruleId;
    this.statuses = other.statuses;
    this.resolutions = other.resolutions;
    this.noAssignee = other.noAssignee;
    this.planned = other.planned;
  }

  public static ReviewQuery create() {
    return new ReviewQuery();
  }

  public static ReviewQuery copy(ReviewQuery reviewQuery) {
    return new ReviewQuery(reviewQuery);
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

  public List<String> getStatuses() {
    return statuses;
  }

  public ReviewQuery addStatus(String status) {
    if (statuses == null) {
      statuses = Lists.newArrayList();
    }
    statuses.add(status);
    return this;
  }

  public Integer getUserId() {
    return userId;
  }

  public ReviewQuery setUserId(Integer userId) {
    this.userId = userId;
    return this;
  }

  public Collection<Integer> getViolationPermanentIds() {
    return violationPermanentIds;
  }

  public ReviewQuery setViolationPermanentIds(List<Integer> l) {
    this.violationPermanentIds = l;
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  public ReviewQuery setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  public List<String> getResolutions() {
    return resolutions;
  }

  public ReviewQuery addResolution(String resolution) {
    if (resolutions == null) {
      resolutions = Lists.newArrayList();
    }
    resolutions.add(resolution);
    return this;
  }

  public Boolean getManualSeverity() {
    return manualSeverity;
  }

  public ReviewQuery setManualSeverity(boolean b) {
    this.manualSeverity = b;
    return this;
  }

  public Boolean getNoAssignee() {
    return noAssignee;
  }

  public ReviewQuery setNoAssignee() {
    this.noAssignee = Boolean.TRUE;
    return this;
  }

  public Boolean getPlanned() {
    return planned;
  }

  public ReviewQuery setPlanned() {
    this.planned = Boolean.TRUE;
    return this;
  }

  boolean needToPartitionQuery() {
    return violationPermanentIds != null && violationPermanentIds.size() > DatabaseUtils.MAX_IN_ELEMENTS;
  }

  ReviewQuery[] partition() {
    List<List<Integer>> partitions = Lists.partition(violationPermanentIds, DatabaseUtils.MAX_IN_ELEMENTS);
    ReviewQuery[] result = new ReviewQuery[partitions.size()];
    for (int index = 0; index < partitions.size(); index++) {
      result[index] = ReviewQuery.copy(this).setViolationPermanentIds(partitions.get(index));
    }

    return result;
  }
}
