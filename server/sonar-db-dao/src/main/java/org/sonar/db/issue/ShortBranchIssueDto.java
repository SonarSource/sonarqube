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

import com.google.common.base.Preconditions;
import java.io.Serializable;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.ShortBranchIssue;
import org.sonar.db.rule.RuleDefinitionDto;

import static com.google.common.base.Preconditions.checkArgument;

public final class ShortBranchIssueDto implements Serializable {

  private Long id;
  private String kee;
  private Integer ruleId;
  private String message;
  private Integer line;
  private String checksum;
  private String status;
  private String resolution;

  // joins
  private String ruleKey;
  private String ruleRepo;

  public String getKey() {
    return getKee();
  }

  public Long getId() {
    return id;
  }

  public ShortBranchIssueDto setId(@Nullable Long id) {
    this.id = id;
    return this;
  }

  public String getKee() {
    return kee;
  }

  public ShortBranchIssueDto setKee(String s) {
    this.kee = s;
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  /**
   * please use setRule(RuleDto rule)
   */
  public ShortBranchIssueDto setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  @CheckForNull
  public String getMessage() {
    return message;
  }

  public ShortBranchIssueDto setMessage(@Nullable String s) {
    checkArgument(s == null || s.length() <= 4000, "Value is too long for issue message: %s", s);
    this.message = s;
    return this;
  }

  @CheckForNull
  public Integer getLine() {
    return line;
  }

  public ShortBranchIssueDto setLine(@Nullable Integer i) {
    checkArgument(i == null || i >= 0, "Value of issue line must be positive: %d", i);
    this.line = i;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public ShortBranchIssueDto setStatus(@Nullable String s) {
    checkArgument(s == null || s.length() <= 20, "Value is too long for issue status: %s", s);
    this.status = s;
    return this;
  }

  @CheckForNull
  public String getResolution() {
    return resolution;
  }

  public ShortBranchIssueDto setResolution(@Nullable String s) {
    checkArgument(s == null || s.length() <= 20, "Value is too long for issue resolution: %s", s);
    this.resolution = s;
    return this;
  }

  @CheckForNull
  public String getChecksum() {
    return checksum;
  }

  public ShortBranchIssueDto setChecksum(@Nullable String s) {
    checkArgument(s == null || s.length() <= 1000, "Value is too long for issue checksum: %s", s);
    this.checksum = s;
    return this;
  }

  public String getRule() {
    return ruleKey;
  }

  public ShortBranchIssueDto setRule(RuleDefinitionDto rule) {
    Preconditions.checkNotNull(rule.getId(), "Rule must be persisted.");
    this.ruleId = rule.getId();
    this.ruleKey = rule.getRuleKey();
    this.ruleRepo = rule.getRepositoryKey();
    return this;
  }

  public RuleKey getRuleKey() {
    return RuleKey.of(ruleRepo, ruleKey);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public static ShortBranchIssue toShortBranchIssue(ShortBranchIssueDto dto) {
    return new ShortBranchIssue(dto.getLine(), dto.getMessage(), dto.getChecksum(), dto.getRuleKey(), dto.getStatus(), dto.getResolution());
  }
}
