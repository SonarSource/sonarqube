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
package org.sonar.db.report;

import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.rules.RuleType;
import org.sonar.db.rule.RuleDto;

public class IssueFindingDto {
  private String kee;
  private String message;
  private int type;
  private String severity;
  private boolean isManualSeverity;
  private String ruleKey;
  private String ruleRepository;
  private String ruleName;
  private String status;
  private String resolution;
  private String fileName;
  private Integer line;
  private String securityStandards;
  private boolean isNewCodeReferenceIssue;
  private long creationDate;
  private List<String> comments;

  public String getStatus() {
    return status;
  }

  @CheckForNull
  public String getRuleName() {
    return ruleName;
  }

  public String getKey() {
    return kee;
  }

  public Set<String> getSecurityStandards() {
    return RuleDto.deserializeSecurityStandardsString(securityStandards);
  }

  public boolean isManualSeverity() {
    return isManualSeverity;
  }

  @CheckForNull
  public String getResolution() {
    return resolution;
  }

  @CheckForNull
  public String getMessage() {
    return message;
  }

  public RuleType getType() {
    return RuleType.valueOf(type);
  }

  public String getSeverity() {
    return severity;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public String getRuleRepository() {
    return ruleRepository;
  }

  @CheckForNull
  public String getFileName() {
    return fileName;
  }

  @CheckForNull
  public Integer getLine() {
    return line;
  }

  public boolean isNewCodeReferenceIssue() {
    return isNewCodeReferenceIssue;
  }

  public long getCreationDate() {
    return creationDate;
  }

  public List<String> getComments() {
    return comments;
  }
}
