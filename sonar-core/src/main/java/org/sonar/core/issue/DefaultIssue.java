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
package org.sonar.core.issue;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DefaultIssue implements Issue {

  private String key;
  private String componentKey;
  private RuleKey ruleKey;
  private String severity;
  private boolean manualSeverity = false;
  private String description;
  private Integer line;
  private Double cost;
  private String status;
  private String resolution;
  private String userLogin;
  private String assignee;
  private boolean manual = false;
  private String checksum;
  private boolean isNew = true;
  private boolean isAlive = true;
  private Map<String, String> attributes = null;
  private String authorLogin = null;
  private FieldDiffs diffs = null;
  private List<IssueComment> newComments = null;

  // functional dates
  private Date creationDate;
  private Date updateDate;
  private Date closeDate;

  // technical dates
  private Date technicalCreationDate;
  private Date technicalUpdateDate;

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

  public boolean manualSeverity() {
    return manualSeverity;
  }

  public DefaultIssue setManualSeverity(boolean b) {
    this.manualSeverity = b;
    return this;
  }

  public String description() {
    return description;
  }

  public DefaultIssue setDescription(@Nullable String s) {
    this.description = StringUtils.abbreviate(StringUtils.trim(s), DESCRIPTION_MAX_SIZE);
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

  public DefaultIssue setStatus(String s) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(s), "Status must be set");
    this.status = s;
    return this;
  }

  public String resolution() {
    return resolution;
  }

  public DefaultIssue setResolution(String s) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(s), "Resolution must be set");
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

  public Date creationDate() {
    return creationDate;
  }

  public DefaultIssue setCreationDate(Date d) {
    this.creationDate = d;
    return this;
  }

  public Date updateDate() {
    return updateDate;
  }

  public DefaultIssue setUpdateDate(@Nullable Date d) {
    this.updateDate = d;
    return this;
  }

  public Date closeDate() {
    return closeDate;
  }

  public DefaultIssue setCloseDate(@Nullable Date d) {
    this.closeDate = d;
    return this;
  }


  /**
   * The date when issue was physically created
   */
  public DefaultIssue setTechnicalCreationDate(@Nullable Date d) {
    this.technicalCreationDate = d;
    return this;
  }

  public Date technicalCreationDate() {
    return technicalCreationDate;
  }

  /**
   * The date when issue was physically updated for the last time
   */

  public DefaultIssue setTechnicalUpdateDate(@Nullable Date d) {
    this.technicalUpdateDate = d;
    return this;
  }

  public Date technicalUpdateDate() {
    return technicalUpdateDate;
  }

  public boolean manual() {
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

  public boolean isAlive() {
    return isAlive;
  }

  public DefaultIssue setAlive(boolean b) {
    isAlive = b;
    return this;
  }

  @CheckForNull
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

  public DefaultIssue setAttributes(@Nullable Map<String, String> map) {
    if (map != null) {
      if (attributes == null) {
        attributes = Maps.newHashMap();
      }
      attributes.putAll(map);
    }
    return this;
  }

  @CheckForNull
  public String authorLogin() {
    return authorLogin;
  }

  public DefaultIssue setAuthorLogin(String authorLogin) {
    this.authorLogin = authorLogin;
    return this;
  }

  public DefaultIssue setFieldDiff(IssueChangeContext context, String field, @Nullable Serializable oldValue, @Nullable Serializable newValue) {
    if (!Objects.equal(oldValue, newValue)) {
      if (diffs == null) {
        diffs = new FieldDiffs();
        diffs.setUserLogin(context.login());
      }
      diffs.setDiff(field, oldValue, newValue);
    }
    return this;
  }

  @CheckForNull
  public FieldDiffs diffs() {
    return diffs;
  }

  public DefaultIssue addComment(IssueComment comment) {
    if (newComments == null) {
      newComments = Lists.newArrayList();
    }
    newComments.add(comment);
    return this;
  }

  public List<IssueComment> newComments() {
    return Objects.firstNonNull(newComments, Collections.<IssueComment>emptyList());
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
