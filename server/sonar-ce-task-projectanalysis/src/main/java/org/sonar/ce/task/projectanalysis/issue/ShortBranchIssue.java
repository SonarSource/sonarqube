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

@Immutable
public class ShortBranchIssue implements Trackable {
  private final String key;
  private final Integer line;
  private final String message;
  private final String lineHash;
  private final RuleKey ruleKey;
  private final String status;
  private final String branchName;
  private final Date creationDate;

  public ShortBranchIssue(String key, @Nullable Integer line, String message, @Nullable String lineHash, RuleKey ruleKey, String status, String branchName, Date creationDate) {
    this.key = key;
    this.line = line;
    this.message = message;
    this.lineHash = lineHash;
    this.ruleKey = ruleKey;
    this.status = status;
    this.branchName = branchName;
    this.creationDate = creationDate;
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

  public String getBranchName() {
    return branchName;
  }

  @Override
  public Date getCreationDate() {
    return creationDate;
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
    ShortBranchIssue other = (ShortBranchIssue) obj;
    return key.equals(other.key);
  }

}
