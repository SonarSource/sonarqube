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
package org.sonar.db.rule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.db.rule.RuleDto.Scope;

import static com.google.common.base.Preconditions.checkArgument;

public class RuleDefinitionDto {

  private Integer id;
  private String repositoryKey;
  private String ruleKey;

  /**
   * Description can be null on external rule, otherwise it should never be null
   */
  private String description;

  /**
   * Description format can be null on external rule, otherwise it should never be null
   */
  private RuleDto.Format descriptionFormat;
  private RuleStatus status;
  private String name;
  private String configKey;

  /**
   * Severity can be null on external rule, otherwise it should never be null
   */
  private Integer severity;

  private boolean isTemplate;

  /**
   * This flag specify that this is an external rule, meaning that generated issues from this rule will be provided by the analyzer without being activated on a quality profile.
   */
  private boolean isExternal;

  /**
   * When an external rule is defined as ad hoc, it means that it's not defined using {@link org.sonar.api.server.rule.RulesDefinition.Context#createExternalRepository(String, String)}.
   * As the opposite, an external rule not being defined as ad hoc is declared by using {@link org.sonar.api.server.rule.RulesDefinition.Context#createExternalRepository(String, String)}.
   * This flag is only used for external rules (it can only be set to true for when {@link #isExternal()} is true)
   */
  private boolean isAdHoc;

  private String language;
  private Integer templateId;
  private String defRemediationFunction;
  private String defRemediationGapMultiplier;
  private String defRemediationBaseEffort;
  private String gapDescription;
  private String systemTags;
  private String securityStandards;
  private int type;
  private Scope scope;

  private RuleKey key;

  private String pluginKey;

  private long createdAt;
  private long updatedAt;

  public RuleKey getKey() {
    if (key == null) {
      key = RuleKey.of(getRepositoryKey(), getRuleKey());
    }
    return key;
  }

  RuleDefinitionDto setKey(RuleKey key) {
    this.key = key;
    setRepositoryKey(key.repository());
    setRuleKey(key.rule());
    return this;
  }

  public Integer getId() {
    return id;
  }

  public RuleDefinitionDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getRepositoryKey() {
    return repositoryKey;
  }

  public RuleDefinitionDto setRepositoryKey(String s) {
    checkArgument(s.length() <= 255, "Rule repository is too long: %s", s);
    this.repositoryKey = s;
    return this;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public RuleDefinitionDto setRuleKey(String s) {
    checkArgument(s.length() <= 200, "Rule key is too long: %s", s);
    this.ruleKey = s;
    return this;
  }

  public RuleDefinitionDto setRuleKey(RuleKey ruleKey) {
    this.repositoryKey = ruleKey.repository();
    this.ruleKey = ruleKey.rule();
    this.key = ruleKey;
    return this;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public RuleDefinitionDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  @CheckForNull
  public RuleDto.Format getDescriptionFormat() {
    return descriptionFormat;
  }

  public RuleDefinitionDto setDescriptionFormat(@Nullable RuleDto.Format descriptionFormat) {
    this.descriptionFormat = descriptionFormat;
    return this;
  }

  public RuleStatus getStatus() {
    return status;
  }

  public RuleDefinitionDto setStatus(@Nullable RuleStatus s) {
    this.status = s;
    return this;
  }

  public String getName() {
    return name;
  }

  public RuleDefinitionDto setName(@Nullable String s) {
    checkArgument(s == null || s.length() <= 255, "Rule name is too long: %s", s);
    this.name = s;
    return this;
  }

  public String getConfigKey() {
    return configKey;
  }

  public RuleDefinitionDto setConfigKey(@Nullable String configKey) {
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

  public RuleDefinitionDto setSeverity(@Nullable String severity) {
    return this.setSeverity(severity != null ? SeverityUtil.getOrdinalFromSeverity(severity) : null);
  }

  public RuleDefinitionDto setSeverity(@Nullable Integer severity) {
    this.severity = severity;
    return this;
  }

  public boolean isTemplate() {
    return isTemplate;
  }

  public RuleDefinitionDto setIsTemplate(boolean isTemplate) {
    this.isTemplate = isTemplate;
    return this;
  }

  public boolean isExternal() {
    return isExternal;
  }

  public RuleDefinitionDto setIsExternal(boolean isExternal) {
    this.isExternal = isExternal;
    return this;
  }

  public boolean isAdHoc() {
    return isAdHoc;
  }

  public RuleDefinitionDto setIsAdHoc(boolean isAdHoc) {
    this.isAdHoc = isAdHoc;
    return this;
  }

  @CheckForNull
  public String getLanguage() {
    return language;
  }

  public RuleDefinitionDto setLanguage(String language) {
    this.language = language;
    return this;
  }

  @CheckForNull
  public Integer getTemplateId() {
    return templateId;
  }

  public boolean isCustomRule() {
    return getTemplateId() != null;
  }

  public RuleDefinitionDto setTemplateId(@Nullable Integer templateId) {
    this.templateId = templateId;
    return this;
  }

  @CheckForNull
  public String getDefRemediationFunction() {
    return defRemediationFunction;
  }

  public RuleDefinitionDto setDefRemediationFunction(@Nullable String defaultRemediationFunction) {
    this.defRemediationFunction = defaultRemediationFunction;
    return this;
  }

  @CheckForNull
  public String getDefRemediationGapMultiplier() {
    return defRemediationGapMultiplier;
  }

  public RuleDefinitionDto setDefRemediationGapMultiplier(@Nullable String defaultRemediationGapMultiplier) {
    this.defRemediationGapMultiplier = defaultRemediationGapMultiplier;
    return this;
  }

  @CheckForNull
  public String getDefRemediationBaseEffort() {
    return defRemediationBaseEffort;
  }

  public RuleDefinitionDto setDefRemediationBaseEffort(@Nullable String defaultRemediationBaseEffort) {
    this.defRemediationBaseEffort = defaultRemediationBaseEffort;
    return this;
  }

  @CheckForNull
  public String getGapDescription() {
    return gapDescription;
  }

  public RuleDefinitionDto setGapDescription(@Nullable String s) {
    this.gapDescription = s;
    return this;
  }

  public Set<String> getSystemTags() {
    return systemTags == null ? new HashSet<>() : new TreeSet<>(Arrays.asList(StringUtils.split(systemTags, ',')));
  }

  private String getSystemTagsField() {
    return systemTags;
  }

  void setSystemTagsField(String s) {
    systemTags = s;
  }

  public RuleDefinitionDto setSystemTags(Set<String> tags) {
    this.systemTags = tags.isEmpty() ? null : StringUtils.join(tags, ',');
    return this;
  }

  public Set<String> getSecurityStandards() {
    return securityStandards == null ? new HashSet<>() : new TreeSet<>(Arrays.asList(StringUtils.split(securityStandards, ',')));
  }

  private String getSecurityStandardsField() {
    return securityStandards;
  }

  void setSecurityStandardsField(String s) {
    securityStandards = s;
  }

  public RuleDefinitionDto setSecurityStandards(Set<String> standards) {
    this.securityStandards = standards.isEmpty() ? null : StringUtils.join(standards, ',');
    return this;
  }

  public int getType() {
    return type;
  }

  public RuleDefinitionDto setType(int type) {
    this.type = type;
    return this;
  }

  public RuleDefinitionDto setType(RuleType type) {
    this.type = type.getDbConstant();
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public RuleDefinitionDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public RuleDefinitionDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Scope getScope() {
    return this.scope;
  }

  public RuleDefinitionDto setScope(Scope scope) {
    this.scope = scope;
    return this;
  }

  @CheckForNull
  public String getPluginKey() {
    return pluginKey;
  }

  public RuleDefinitionDto setPluginKey(@Nullable String pluginKey) {
    this.pluginKey = pluginKey;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RuleDefinitionDto)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    RuleDefinitionDto other = (RuleDefinitionDto) obj;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(id)
      .toHashCode();
  }

  public static RuleDto createFor(RuleKey key) {
    return new RuleDto()
      .setId(new HashCodeBuilder(17, 37).append(key.rule()).append(key.repository()).toHashCode())
      .setRepositoryKey(key.repository())
      .setRuleKey(key.rule());
  }

  @Override
  public String toString() {
    return "RuleDefinitionDto{" +
      "id=" + id +
      ", repositoryKey='" + repositoryKey + '\'' +
      ", ruleKey='" + ruleKey + '\'' +
      ", description='" + description + '\'' +
      ", descriptionFormat=" + descriptionFormat +
      ", status=" + status +
      ", name='" + name + '\'' +
      ", configKey='" + configKey + '\'' +
      ", severity=" + severity +
      ", isTemplate=" + isTemplate +
      ", language='" + language + '\'' +
      ", templateId=" + templateId +
      ", defRemediationFunction='" + defRemediationFunction + '\'' +
      ", defRemediationGapMultiplier='" + defRemediationGapMultiplier + '\'' +
      ", defRemediationBaseEffort='" + defRemediationBaseEffort + '\'' +
      ", gapDescription='" + gapDescription + '\'' +
      ", systemTags='" + systemTags + '\'' +
      ", type=" + type +
      ", key=" + key +
      ", createdAt=" + createdAt +
      ", updatedAt=" + updatedAt +
      ", scope=" + scope +
      '}';
  }
}
