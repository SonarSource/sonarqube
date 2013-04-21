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
package org.sonar.api.workflow.internal;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.workflow.Comment;
import org.sonar.api.workflow.MutableReview;
import org.sonar.api.utils.KeyValueFormat;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @since 3.1
 */
@Beta
public final class DefaultReview implements MutableReview {

  private Long violationId;
  private Long reviewId;
  private String ruleRepositoryKey;
  private String ruleKey;
  private String ruleName;
  private Long line;
  private boolean switchedOff = false;
  private boolean manual = false;
  private String message;
  private String status;
  private String resolution;
  private String severity;
  private Map<String, String> properties;
  private List<Comment> newComments;

  public Long getViolationId() {
    return violationId;
  }

  public DefaultReview setViolationId(Long violationId) {
    this.violationId = violationId;
    return this;
  }

  public Long getReviewId() {
    return reviewId;
  }

  public DefaultReview setReviewId(Long reviewId) {
    this.reviewId = reviewId;
    return this;
  }

  public String getRuleRepositoryKey() {
    return ruleRepositoryKey;
  }

  public DefaultReview setRuleRepositoryKey(String s) {
    this.ruleRepositoryKey = s;
    return this;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public DefaultReview setRuleKey(String s) {
    this.ruleKey = s;
    return this;
  }

  public String getRuleName() {
    return ruleName;
  }

  public DefaultReview setRuleName(String s) {
    this.ruleName = s;
    return this;
  }

  public Long getLine() {
    return line;
  }

  public DefaultReview setLine(Long line) {
    this.line = line;
    return this;
  }

  public boolean isSwitchedOff() {
    return switchedOff;
  }

  public DefaultReview setSwitchedOff(boolean b) {
    this.switchedOff = b;
    return this;
  }

  public boolean isManual() {
    return manual;
  }

  public DefaultReview setManual(boolean manual) {
    this.manual = manual;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public DefaultReview setMessage(String message) {
    this.message = message;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public DefaultReview setStatus(String s) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(s));
    this.status = s;
    return this;
  }

  public String getResolution() {
    return resolution;
  }

  public DefaultReview setResolution(@Nullable String s) {
    this.resolution = s;
    return this;
  }

  public String getSeverity() {
    return severity;
  }

  public DefaultReview setSeverity(String s) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(s));
    this.severity = s;
    return this;
  }

  public Map<String, String> getProperties() {
    if (properties == null) {
      return Collections.emptyMap();
    }
    return properties;
  }

  public DefaultReview setProperties(Map<String, String> properties) {
    this.properties = properties;
    return this;
  }

  public DefaultReview setPropertiesAsString(@Nullable String s) {
    this.properties = (s == null ? null : KeyValueFormat.parse(s));
    return this;
  }

  public Comment createComment() {
    if (newComments == null) {
      newComments = Lists.newArrayList();
    }
    Comment comment = new DefaultComment();
    newComments.add(comment);
    return comment;
  }

  public List<Comment> getNewComments() {
    if (newComments == null) {
      return Collections.emptyList();
    }
    return newComments;
  }

  public DefaultReview setProperty(String key, @Nullable String value) {
    if (properties == null) {
      // keeping entries ordered by key allows to have consistent behavior in unit tests
      properties = Maps.newLinkedHashMap();
    }
    properties.put(key, value);
    return this;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).toString();
  }
}