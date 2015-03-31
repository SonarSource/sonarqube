/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.rule;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.core.persistence.Dto;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.*;

public class RuleDto extends Dto<RuleKey> {

  public static final Integer DISABLED_CHARACTERISTIC_ID = -1;

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
  private Integer subCharacteristicId;
  private Integer defaultSubCharacteristicId;
  private String remediationFunction;
  private String defaultRemediationFunction;
  private String remediationCoefficient;
  private String defaultRemediationCoefficient;
  private String remediationOffset;
  private String defaultRemediationOffset;
  private String effortToFixDescription;
  private String tags;
  private String systemTags;

  private RuleKey key;

  @Override
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

  public RuleDto setRepositoryKey(String repositoryKey) {
    this.repositoryKey = repositoryKey;
    return this;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public RuleDto setRuleKey(String ruleKey) {
    this.ruleKey = ruleKey;
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

  public RuleDto setName(@Nullable String name) {
    this.name = name;
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
  public Integer getSubCharacteristicId() {
    return subCharacteristicId;
  }

  public RuleDto setSubCharacteristicId(@Nullable Integer subCharacteristicId) {
    this.subCharacteristicId = subCharacteristicId;
    return this;
  }

  @CheckForNull
  public Integer getDefaultSubCharacteristicId() {
    return defaultSubCharacteristicId;
  }

  public RuleDto setDefaultSubCharacteristicId(@Nullable Integer defaultSubCharacteristicId) {
    this.defaultSubCharacteristicId = defaultSubCharacteristicId;
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
    return defaultRemediationFunction;
  }

  public RuleDto setDefaultRemediationFunction(@Nullable String defaultRemediationFunction) {
    this.defaultRemediationFunction = defaultRemediationFunction;
    return this;
  }

  @CheckForNull
  public String getRemediationCoefficient() {
    return remediationCoefficient;
  }

  public RuleDto setRemediationCoefficient(@Nullable String remediationCoefficient) {
    this.remediationCoefficient = remediationCoefficient;
    return this;
  }

  @CheckForNull
  public String getDefaultRemediationCoefficient() {
    return defaultRemediationCoefficient;
  }

  public RuleDto setDefaultRemediationCoefficient(@Nullable String defaultRemediationCoefficient) {
    this.defaultRemediationCoefficient = defaultRemediationCoefficient;
    return this;
  }

  @CheckForNull
  public String getRemediationOffset() {
    return remediationOffset;
  }

  public RuleDto setRemediationOffset(@Nullable String remediationOffset) {
    this.remediationOffset = remediationOffset;
    return this;
  }

  @CheckForNull
  public String getDefaultRemediationOffset() {
    return defaultRemediationOffset;
  }

  public RuleDto setDefaultRemediationOffset(@Nullable String defaultRemediationOffset) {
    this.defaultRemediationOffset = defaultRemediationOffset;
    return this;
  }

  @CheckForNull
  public String getEffortToFixDescription() {
    return effortToFixDescription;
  }

  public RuleDto setEffortToFixDescription(@Nullable String s) {
    this.effortToFixDescription = s;
    return this;
  }

  public Set<String> getTags() {
    return tags == null ?
      new HashSet<String>() :
      new TreeSet<String>(Arrays.asList(StringUtils.split(tags, ',')));
  }

  public Set<String> getSystemTags() {
    return systemTags == null ?
      new HashSet<String>() :
      new TreeSet<String>(Arrays.asList(StringUtils.split(systemTags, ',')));
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
    this.tags = tags.isEmpty() ? null : StringUtils.join(tags, ',');
    return this;
  }

  public RuleDto setSystemTags(Set<String> tags) {
    this.systemTags = tags.isEmpty() ? null : StringUtils.join(tags, ',');
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
