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
package org.sonar.core.issue;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class DefaultIssue implements Issue, Serializable {

  private static final Set<String> RESOLUTIONS = ImmutableSet.of(RESOLUTION_FALSE_POSITIVE, RESOLUTION_FIXED);
  private static final Set<String> STATUSES = ImmutableSet.of(STATUS_OPEN, STATUS_CLOSED, STATUS_REOPENED, STATUS_RESOLVED);

  private String key;
  private String componentKey;
  private RuleKey ruleKey;
  private String severity;
  private boolean manualSeverity = false;
  private String title;
  private String description;
  private Integer line;
  private Double cost;
  private String status;
  private String resolution;
  private String userLogin;
  private String assignee;
  private Date createdAt;
  private Date updatedAt;
  private Date closedAt;
  private boolean manual = false;
  private String checksum;
  private boolean isNew = true;
  private Map<String, String> attributes = null;

  public String key() {
    return key;
  }

  public DefaultIssue setKey(String key) {
    this.key = key;
    return this;
  }

  public String componentKey() {
    return componentKey;
  }

  public DefaultIssue setComponentKey(String s) {
    this.componentKey = s;
    return this;
  }

  public RuleKey ruleKey() {
    return ruleKey;
  }

  public DefaultIssue setRuleKey(RuleKey k) {
    this.ruleKey = k;
    return this;
  }

  public String severity() {
    return severity;
  }

  public DefaultIssue setSeverity(@Nullable String s) {
    Preconditions.checkArgument(s == null || Severity.ALL.contains(s), "Not a valid severity: " + s);
    this.severity = s;
    return this;
  }

  public boolean isManualSeverity() {
    return manualSeverity;
  }

  public DefaultIssue setManualSeverity(boolean b) {
    this.manualSeverity = b;
    return this;
  }

  public String title() {
    return title;
  }

  public DefaultIssue setTitle(@Nullable String title) {
    this.title = title;
    return this;
  }

  public String description() {
    return description;
  }

  public DefaultIssue setDescription(@Nullable String s) {
    this.description = StringUtils.abbreviate(s, DESCRIPTION_MAX_SIZE);
    return this;
  }

  public Integer line() {
    return line;
  }

  public DefaultIssue setLine(@Nullable Integer l) {
    Preconditions.checkArgument(l == null || l > 0, "Line must be null or greater than zero (got " + l + ")");
    this.line = l;
    return this;
  }

  public Double cost() {
    return cost;
  }

  public DefaultIssue setCost(@Nullable Double c) {
    Preconditions.checkArgument(c == null || c >= 0, "Cost must be positive (got " + c + ")");
    this.cost = c;
    return this;
  }

  public String status() {
    return status;
  }

  public DefaultIssue setStatus(@Nullable String s) {
    Preconditions.checkArgument(s == null || STATUSES.contains(s), "Not a valid status: " + s);
    this.status = s;
    return this;
  }

  public String resolution() {
    return resolution;
  }

  public DefaultIssue setResolution(@Nullable String s) {
    Preconditions.checkArgument(s == null || RESOLUTIONS.contains(s), "Not a valid resolution: " + s);
    this.resolution = s;
    return this;
  }

  public String userLogin() {
    return userLogin;
  }

  public DefaultIssue setUserLogin(@Nullable String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  public String assignee() {
    return assignee;
  }

  public DefaultIssue setAssignee(@Nullable String s) {
    this.assignee = s;
    return this;
  }

  public Date createdAt() {
    return createdAt;
  }

  public DefaultIssue setCreatedAt(@Nullable Date d) {
    this.createdAt = d;
    return this;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public DefaultIssue setUpdatedAt(@Nullable Date d) {
    this.updatedAt = d;
    return this;
  }

  public Date closedAt() {
    return closedAt;
  }

  public DefaultIssue setClosedAt(@Nullable Date d) {
    this.closedAt = d;
    return this;
  }

  public boolean isManual() {
    return manual;
  }

  public DefaultIssue setManual(boolean b) {
    this.manual = b;
    return this;
  }

  public String getChecksum() {
    return checksum;
  }

  public DefaultIssue setChecksum(@Nullable String s) {
    this.checksum = s;
    return this;
  }

  public boolean isNew() {
    return isNew;
  }

  public DefaultIssue setNew(boolean b) {
    isNew = b;
    return this;
  }

  public String attribute(String key) {
    return attributes == null ? null : attributes.get(key);
  }

  public DefaultIssue setAttribute(String key, @Nullable String value) {
    if (attributes == null) {
      attributes = Maps.newHashMap();
    }
    if (value == null) {
      attributes.remove(key);
    } else {
      attributes.put(key, value);
    }
    return this;
  }

  public Map<String, String> attributes() {
    return attributes == null ? Collections.<String, String>emptyMap() : ImmutableMap.copyOf(attributes);
  }

  public DefaultIssue setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultIssue that = (DefaultIssue) o;
    if (key != null ? !key.equals(that.key) : that.key != null) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return key != null ? key.hashCode() : 0;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }


}
