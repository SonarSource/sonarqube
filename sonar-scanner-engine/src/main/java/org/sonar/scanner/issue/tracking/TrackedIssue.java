/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.issue.tracking;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.tracking.Trackable;

public class TrackedIssue implements Trackable, Serializable {
  private static final long serialVersionUID = -1755017079070964287L;

  private RuleKey ruleKey;
  private String key;
  private String severity;
  private Integer startLine;
  private Integer startLineOffset;
  private Integer endLine;
  private Integer endLineOffset;
  private Double gap;
  private boolean isNew;
  private Date creationDate;
  private String resolution;
  private String status;
  private String assignee;
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

  public TrackedIssue setMessage(String message) {
    this.message = message;
    return this;
  }

  /**
   * Component key shared by all part of SonarQube (batch, server, WS...). 
   * It doesn't include the branch.
   */
  public String componentKey() {
    return componentKey;
  }

  /**
   * Component key shared by all part of SonarQube (batch, server, WS...). 
   * It doesn't include the branch.
   */
  public TrackedIssue setComponentKey(String componentKey) {
    this.componentKey = componentKey;
    return this;
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

  public TrackedIssue setStartLine(Integer startLine) {
    this.startLine = startLine;
    return this;
  }

  public Integer startLineOffset() {
    return startLineOffset;
  }

  public TrackedIssue setStartLineOffset(Integer startLineOffset) {
    this.startLineOffset = startLineOffset;
    return this;
  }

  public Integer endLine() {
    return endLine;
  }

  public TrackedIssue setEndLine(Integer endLine) {
    this.endLine = endLine;
    return this;
  }

  public Integer endLineOffset() {
    return endLineOffset;
  }

  public TrackedIssue setEndLineOffset(Integer endLineOffset) {
    this.endLineOffset = endLineOffset;
    return this;
  }

  public TrackedIssue setKey(String key) {
    this.key = key;
    return this;
  }

  public String assignee() {
    return assignee;
  }

  public TrackedIssue setAssignee(String assignee) {
    this.assignee = assignee;
    return this;
  }

  public String resolution() {
    return resolution;
  }

  public TrackedIssue setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  public String status() {
    return status;
  }

  public TrackedIssue setStatus(String status) {
    this.status = status;
    return this;
  }

  @Override
  public RuleKey getRuleKey() {
    return ruleKey;
  }

  @Override
  public String getStatus() {
    return status;
  }

  public String severity() {
    return severity;
  }

  public Double gap() {
    return gap;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public boolean isNew() {
    return isNew;
  }

  public TrackedIssue setNew(boolean isNew) {
    this.isNew = isNew;
    return this;
  }

  public Date creationDate() {
    return creationDate;
  }

  public TrackedIssue setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
    return this;
  }

  public TrackedIssue setRuleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public TrackedIssue setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  public TrackedIssue setGap(Double gap) {
    this.gap = gap;
    return this;
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + ((key == null) ? 0 : key.hashCode());
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
