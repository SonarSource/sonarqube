/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.component.KeyType;

@Immutable
public class SiblingIssue implements Trackable {
  private final String key;
  private final Integer line;
  private final String message;
  private final String lineHash;
  private final RuleKey ruleKey;
  private final String status;
  private final String branchKey;
  private final KeyType branchType;
  private final Date updateDate;

  public SiblingIssue(String key, @Nullable Integer line, String message, @Nullable String lineHash, RuleKey ruleKey, String status, String branchKey, KeyType branchType,
    Date updateDate) {
    this.key = key;
    this.line = line;
    this.message = message;
    this.lineHash = lineHash;
    this.ruleKey = ruleKey;
    this.status = status;
    this.branchKey = branchKey;
    this.branchType = branchType;
    this.updateDate = updateDate;
  }

  public String getKey() {
    return key;
  }

  @CheckForNull
  @Override
  public Integer getLine() {
    return line;
  }

  @Override
  public String getMessage() {
    return message;
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

  public String getBranchKey() {
    return branchKey;
  }

  public KeyType getBranchType() {
    return branchType;
  }

  @Override
  public Date getUpdateDate() {
    return updateDate;
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
