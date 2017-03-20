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
package org.sonar.db.rule;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

import static com.google.common.base.Preconditions.checkArgument;

public class RuleMetadataDto {
  private int ruleId;
  private String noteData;
  private String noteUserLogin;
  private Date noteCreatedAt;
  private Date noteUpdatedAt;
  private String remediationFunction;
  private String remediationGapMultiplier;
  private String remediationBaseEffort;
  private String tags;
  private long updatedAt;

  public int getRuleId() {
    return ruleId;
  }

  public RuleMetadataDto setRuleId(int ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  @CheckForNull
  public String getNoteData() {
    return noteData;
  }

  public RuleMetadataDto setNoteData(@Nullable String s) {
    this.noteData = s;
    return this;
  }

  @CheckForNull
  public String getNoteUserLogin() {
    return noteUserLogin;
  }

  public RuleMetadataDto setNoteUserLogin(@Nullable String noteUserLogin) {
    this.noteUserLogin = noteUserLogin;
    return this;
  }

  @CheckForNull
  public Date getNoteCreatedAt() {
    return noteCreatedAt;
  }

  public RuleMetadataDto setNoteCreatedAt(@Nullable Date noteCreatedAt) {
    this.noteCreatedAt = noteCreatedAt;
    return this;
  }

  @CheckForNull
  public Date getNoteUpdatedAt() {
    return noteUpdatedAt;
  }

  public RuleMetadataDto setNoteUpdatedAt(@Nullable Date noteUpdatedAt) {
    this.noteUpdatedAt = noteUpdatedAt;
    return this;
  }

  @CheckForNull
  public String getRemediationFunction() {
    return remediationFunction;
  }

  public RuleMetadataDto setRemediationFunction(@Nullable String remediationFunction) {
    this.remediationFunction = remediationFunction;
    return this;
  }

  @CheckForNull
  public String getRemediationGapMultiplier() {
    return remediationGapMultiplier;
  }

  public RuleMetadataDto setRemediationGapMultiplier(@Nullable String remediationGapMultiplier) {
    this.remediationGapMultiplier = remediationGapMultiplier;
    return this;
  }

  @CheckForNull
  public String getRemediationBaseEffort() {
    return remediationBaseEffort;
  }

  public RuleMetadataDto setRemediationBaseEffort(@Nullable String remediationBaseEffort) {
    this.remediationBaseEffort = remediationBaseEffort;
    return this;
  }

  public Set<String> getTags() {
    return tags == null ? new HashSet<>() : new TreeSet<>(Arrays.asList(StringUtils.split(tags, ',')));
  }

  public RuleMetadataDto setTags(Set<String> tags) {
    String raw = tags.isEmpty() ? null : StringUtils.join(tags, ',');
    checkArgument(raw == null || raw.length() <= 4000, "Rule tags are too long: %s", raw);
    this.tags = raw;
    return this;
  }

  private String getTagsField() {
    return tags;
  }

  private void setTagsField(String s) {
    tags = s;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public RuleMetadataDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
