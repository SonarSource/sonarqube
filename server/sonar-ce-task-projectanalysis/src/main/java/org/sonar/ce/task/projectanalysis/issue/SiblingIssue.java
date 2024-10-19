/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.tracking.Trackable;
import org.sonar.db.component.BranchType;

@Immutable
public class SiblingIssue implements Trackable {
  private final String key;
  private final Integer line;
  private final String message;
  private final String lineHash;
  private final RuleKey ruleKey;
  private final String status;
  private final String prKey;
  private final BranchType branchType;
  private final Date updateDate;
  private final String cveId;

  SiblingIssue(String key, @Nullable Integer line, @Nullable String message, @Nullable String lineHash, RuleKey ruleKey, String status, String prKey, BranchType branchType,
    Date updateDate) {
    this(key, line, message, lineHash, ruleKey, status, prKey, branchType, updateDate, null);
  }

  SiblingIssue(String key, @Nullable Integer line, @Nullable String message, @Nullable String lineHash, RuleKey ruleKey, String status, String prKey, BranchType branchType,
    Date updateDate, @Nullable String cveId) {
    this.key = key;
    this.line = line;
    this.message = message;
    this.lineHash = lineHash;
    this.ruleKey = ruleKey;
    this.status = status;
    this.prKey = prKey;
    this.branchType = branchType;
    this.updateDate = updateDate;
    this.cveId = cveId;
  }

  public String getKey() {
    return key;
  }

  @CheckForNull
  @Override
  public Integer getLine() {
    return line;
  }

  @CheckForNull
  @Override
  public String getMessage() {
    return message;
  }

  public BranchType getBranchType() {
    return branchType;
  }

  @CheckForNull
  @Override
  public String getLineHash() {
    return lineHash;
  }

  @Override
  public RuleKey getRuleKey() {
    return ruleKey;
  }

  @Override
  public String getStatus() {
    return status;
  }

  public String getPrKey() {
    return prKey;
  }

  @Override
  public Date getUpdateDate() {
    return updateDate;
  }

  @CheckForNull
  @Override
  public String getCveId() {
    return cveId;
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SiblingIssue other = (SiblingIssue) obj;
    return key.equals(other.key);
  }

}
