/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.issue.tracking;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import com.google.common.base.Preconditions;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.sonar.core.issue.tracking.Trackable;

import java.io.Serializable;
import java.util.Date;

import org.sonar.api.rule.RuleKey;

public class TrackedIssue implements Trackable, Serializable {
  private static final long serialVersionUID = -1755017079070964287L;

  private RuleKey ruleKey;
  private String key;
  private String severity;
  private Integer startLine;
  private Integer startLineOffset;
  private Integer endLine;
  private Integer endLineOffset;
  private Double effortToFix;
  private boolean isNew;
  private Date creationDate;
  private String resolution;
  private String status;
  private String assignee;
  private String reporter;
  private String componentKey;
  private String message;

  private transient FileHashes hashes;

  public TrackedIssue() {
    hashes = null;
  }

  public TrackedIssue(@Nullable FileHashes hashes) {
    this.hashes = hashes;
  }

  @Override
  @CheckForNull
  public String getLineHash() {
    if (getLine() == null || hashes == null) {
      return null;
    }

    int line = getLine();
    Preconditions.checkState(line <= hashes.length(), "Invalid line number for issue %s. File has only %s line(s)", this, hashes.length());

    return hashes.getHash(line);
  }

  @Override
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String componentKey() {
    return componentKey;
  }

  public void setComponentKey(String componentKey) {
    this.componentKey = componentKey;
  }

  public String key() {
    return key;
  }

  public Integer startLine() {
    return startLine;
  }

  @Override
  public Integer getLine() {
    return startLine;
  }

  public void setStartLine(Integer startLine) {
    this.startLine = startLine;
  }

  public Integer startLineOffset() {
    return startLineOffset;
  }

  public void setStartLineOffset(Integer startLineOffset) {
    this.startLineOffset = startLineOffset;
  }

  public Integer endLine() {
    return endLine;
  }

  public void setEndLine(Integer endLine) {
    this.endLine = endLine;
  }

  public Integer endLineOffset() {
    return endLineOffset;
  }

  public void setEndLineOffset(Integer endLineOffset) {
    this.endLineOffset = endLineOffset;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String assignee() {
    return assignee;
  }

  public void setAssignee(String assignee) {
    this.assignee = assignee;
  }

  public String reporter() {
    return reporter;
  }

  public void setReporter(String reporter) {
    this.reporter = reporter;
  }

  public String resolution() {
    return resolution;
  }

  public void setResolution(String resolution) {
    this.resolution = resolution;
  }

  public String status() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public RuleKey getRuleKey() {
    return ruleKey;
  }

  public String severity() {
    return severity;
  }

  public Double effortToFix() {
    return effortToFix;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public boolean isNew() {
    return isNew;
  }

  public void setNew(boolean isNew) {
    this.isNew = isNew;
  }

  public Date creationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public void setRuleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public void setEffortToFix(Double effortToFix) {
    this.effortToFix = effortToFix;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    TrackedIssue other = (TrackedIssue) obj;
    if (key == null) {
      if (other.key != null) {
        return false;
      }
    } else if (!key.equals(other.key)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

}
