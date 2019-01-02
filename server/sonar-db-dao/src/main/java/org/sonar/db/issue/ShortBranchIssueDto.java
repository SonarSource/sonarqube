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
package org.sonar.db.issue;

import java.io.Serializable;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;

public final class ShortBranchIssueDto implements Serializable {

  private String kee;
  private String message;
  private Integer line;
  private String checksum;
  private String status;
  private Long issueCreationDate;

  // joins
  private String ruleKey;
  private String ruleRepo;
  private String branchName;

  public String getKey() {
    return kee;
  }

  public ShortBranchIssueDto setKee(String s) {
    this.kee = s;
    return this;
  }

  @CheckForNull
  public String getMessage() {
    return message;
  }

  public ShortBranchIssueDto setMessage(@Nullable String s) {
    this.message = s;
    return this;
  }

  @CheckForNull
  public Integer getLine() {
    return line;
  }

  public ShortBranchIssueDto setLine(@Nullable Integer i) {
    this.line = i;
    return this;
  }

  public String getBranchName() {
    return branchName;
  }

  public ShortBranchIssueDto setBranchName(String s) {
    this.branchName = s;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public ShortBranchIssueDto setStatus(@Nullable String s) {
    this.status = s;
    return this;
  }

  @CheckForNull
  public String getChecksum() {
    return checksum;
  }

  public ShortBranchIssueDto setChecksum(@Nullable String s) {
    this.checksum = s;
    return this;
  }

  public void setRuleRepo(String ruleRepo) {
    this.ruleRepo = ruleRepo;
  }

  public void setRuleKey(String ruleKey) {
    this.ruleKey = ruleKey;
  }

  public RuleKey getRuleKey() {
    return RuleKey.of(ruleRepo, ruleKey);
  }

  public Long getIssueCreationDate() {
    return issueCreationDate;
  }

  public ShortBranchIssueDto setIssueCreationDate(Long issueCreationDate) {
    this.issueCreationDate = issueCreationDate;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

}
