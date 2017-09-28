/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.db.issue;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.tracking.Trackable;

@Immutable
public class ShortBranchIssue implements Trackable {
  private final Integer line;
  private final String message;
  private final String lineHash;
  private final RuleKey ruleKey;
  private final String status;
  private final String resolution;

  public ShortBranchIssue(ShortBranchIssueDto dto) {
    this.line = dto.getLine();
    this.message = dto.getMessage();
    this.lineHash = dto.getChecksum();
    this.ruleKey = dto.getRuleKey();
    this.status = dto.getStatus();
    this.resolution = dto.getResolution();
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

  public String getStatus() {
    return status;
  }

  public String getResolution() {
    return resolution;
  }
}
