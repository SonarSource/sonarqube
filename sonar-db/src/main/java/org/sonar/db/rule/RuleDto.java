/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;

import static com.google.common.base.Preconditions.checkArgument;

public class RuleDto {

  public enum Format {
    HTML, MARKDOWN
  }

  private Integer id;
  private String repositoryKey;
  private String ruleKey;
  private String description;
  private Format descriptionFormat;
  private RuleStatus status;
  private String name;
  private String configKey;
  private Integer severity;
  private boolean isTemplate;
  private String language;
  private Integer templateId;
  private String noteData;
  private String noteUserLogin;
  private Date noteCreatedAt;
  private Date noteUpdatedAt;
  private String remediationFunction;
  private String defRemediationFunction;
  private String remediationGapMultiplier;
  private String defRemediationGapMultiplier;
  private String remediationBaseEffort;
  private String defRemediationBaseEffort;
  private String gapDescription;
  private String tags;
  private String systemTags;
  private int type;

  private RuleKey key;

  private long createdAt;
  private long updatedAt;

  public RuleKey getKey() {
    if (key == null) {
      key = RuleKey.of(getRepositoryKey(), getRuleKey());
    }
    return key;
  }

  public Integer getId() {
    return id;
  }

  public RuleDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getRepositoryKey() {
    return repositoryKey;
  }

  public RuleDto setRepositoryKey(String s) {
    checkArgument(s.length() <= 255, "Rule repository is too long: %s", s);
    this.repositoryKey = s;
    return this;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public RuleDto setRuleKey(String s) {
    checkArgument(s.length() <= 200, "Rule key is too long: %s", s);
    this.ruleKey = s;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public RuleDto setDescription(String description) {
    this.description = description;
    return this;
  }

  public Format getDescriptionFormat() {
    return descriptionFormat;
  }

  public RuleDto setDescriptionFormat(Format descriptionFormat) {
    this.descriptionFormat = descriptionFormat;
    return this;
  }

  public RuleStatus getStatus() {
    return status;
  }

  public RuleDto setStatus(@Nullable RuleStatus s) {
    this.status = s;
    return this;
  }

  public String getName() {
    return name;
  }

  public RuleDto setName(@Nullable String s) {
    checkArgument(s== null || s.length() <= 255, "Rule name is too long: %s", s);
    this.name = s;
    return this;
  }

  public String getConfigKey() {
    return configKey;
  }

  public RuleDto setConfigKey(@Nullable String configKey) {
    this.configKey = configKey;
    return this;
  }

  @CheckForNull
  public Integer getSeverity() {
    return severity;
  }

  @CheckForNull
  public String getSeverityString() {
    return severity != null ? SeverityUtil.getSeverityFromOrdinal(severity) : null;
  }

  public RuleDto setSeverity(@Nullable String severity) {
    return this.setSeverity(severity != null ? SeverityUtil.getOrdinalFromSeverity(severity) : null);
  }

  public RuleDto setSeverity(@Nullable Integer severity) {
    this.severity = severity;
    return this;
  }

  public boolean isTemplate() {
    return isTemplate;
  }

  public RuleDto setIsTemplate(boolean isTemplate) {
    this.isTemplate = isTemplate;
    return this;
  }

  @CheckForNull
  public String getLanguage() {
    return language;
  }

  public RuleDto setLanguage(String language) {
    this.language = language;
    return this;
  }

  @CheckForNull
  public Integer getTemplateId() {
    return templateId;
  }

  public RuleDto setTemplateId(@Nullable Integer templateId) {
    this.templateId = templateId;
    return this;
  }

  public String getNoteData() {
    return noteData;
  }

  public RuleDto setNoteData(String noteData) {
    this.noteData = noteData;
    return this;
  }

  public String getNoteUserLogin() {
    return noteUserLogin;
  }

  public RuleDto setNoteUserLogin(String noteUserLogin) {
    this.noteUserLogin = noteUserLogin;
    return this;
  }

  public Date getNoteCreatedAt() {
    return noteCreatedAt;
  }

  public RuleDto setNoteCreatedAt(Date noteCreatedAt) {
    this.noteCreatedAt = noteCreatedAt;
    return this;
  }

  public Date getNoteUpdatedAt() {
    return noteUpdatedAt;
  }

  public RuleDto setNoteUpdatedAt(Date noteUpdatedAt) {
    this.noteUpdatedAt = noteUpdatedAt;
    return this;
  }

  @CheckForNull
  public String getRemediationFunction() {
    return remediationFunction;
  }

  public RuleDto setRemediationFunction(@Nullable String remediationFunction) {
    this.remediationFunction = remediationFunction;
    return this;
  }

  @CheckForNull
  public String getDefaultRemediationFunction() {
    return defRemediationFunction;
  }

  public RuleDto setDefaultRemediationFunction(@Nullable String defaultRemediationFunction) {
    this.defRemediationFunction = defaultRemediationFunction;
    return this;
  }

  @CheckForNull
  public String getRemediationGapMultiplier() {
    return remediationGapMultiplier;
  }

  public RuleDto setRemediationGapMultiplier(@Nullable String remediationGapMultiplier) {
    this.remediationGapMultiplier = remediationGapMultiplier;
    return this;
  }

  @CheckForNull
  public String getDefaultRemediationGapMultiplier() {
    return defRemediationGapMultiplier;
  }

  public RuleDto setDefaultRemediationGapMultiplier(@Nullable String defaultRemediationGapMultiplier) {
    this.defRemediationGapMultiplier = defaultRemediationGapMultiplier;
    return this;
  }

  @CheckForNull
  public String getRemediationBaseEffort() {
    return remediationBaseEffort;
  }

  public RuleDto setRemediationBaseEffort(@Nullable String remediationBaseEffort) {
    this.remediationBaseEffort = remediationBaseEffort;
    return this;
  }

  @CheckForNull
  public String getDefaultRemediationBaseEffort() {
    return defRemediationBaseEffort;
  }

  public RuleDto setDefaultRemediationBaseEffort(@Nullable String defaultRemediationBaseEffort) {
    this.defRemediationBaseEffort = defaultRemediationBaseEffort;
    return this;
  }

  @CheckForNull
  public String getGapDescription() {
    return gapDescription;
  }

  public RuleDto setGapDescription(@Nullable String s) {
    this.gapDescription = s;
    return this;
  }

  public Set<String> getTags() {
    return tags == null ? new HashSet<String>() : new TreeSet<>(Arrays.asList(StringUtils.split(tags, ',')));
  }

  public Set<String> getSystemTags() {
    return systemTags == null ? new HashSet<String>() : new TreeSet<>(Arrays.asList(StringUtils.split(systemTags, ',')));
  }

  private String getTagsField() {
    return tags;
  }

  private String getSystemTagsField() {
    return systemTags;
  }

  private void setTagsField(String s) {
    tags = s;
  }

  private void setSystemTagsField(String s) {
    systemTags = s;
  }

  public RuleDto setTags(Set<String> tags) {
    String raw = tags.isEmpty() ? null : StringUtils.join(tags, ',');
    checkArgument(raw == null || raw.length() <= 4000, "Rule tags are too long: %s", raw);
    this.tags = raw;
    return this;
  }

  public RuleDto setSystemTags(Set<String> tags) {
    this.systemTags = tags.isEmpty() ? null : StringUtils.join(tags, ',');
    return this;
  }

  public int getType() {
    return type;
  }

  public RuleDto setType(int type) {
    this.type = type;
    return this;
  }

  public RuleDto setType(RuleType type) {
    this.type = type.getDbConstant();
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public RuleDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public RuleDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RuleDto)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    RuleDto other = (RuleDto) obj;
    return new EqualsBuilder()
      .append(repositoryKey, other.getRepositoryKey())
      .append(ruleKey, other.getRuleKey())
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(repositoryKey)
      .append(ruleKey)
      .toHashCode();
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }

  public static RuleDto createFor(RuleKey key) {
    return new RuleDto()
      .setRepositoryKey(key.repository())
      .setRuleKey(key.rule());
  }

}
