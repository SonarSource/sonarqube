/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.qualityprofile;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.SeverityUtil;

import static java.util.Objects.requireNonNull;

public class ActiveRuleDto {

  public static final String INHERITED = ActiveRule.INHERITED;
  public static final String OVERRIDES = ActiveRule.OVERRIDES;
  private static final Gson GSON = new Gson();
  private static final Type TYPE = new TypeToken<Map<SoftwareQuality, Severity>>() {
  }.getType();

  private String uuid;
  private String profileUuid;
  private String ruleUuid;
  private Integer severity;
  private String inheritance;
  private String impacts;
  private long createdAt;
  private long updatedAt;
  private boolean prioritizedRule;

  // These fields do not exist in db, it's only retrieve by joins
  private String repository;
  private String ruleField;
  private String securityStandards;
  private boolean isExternal;

  private String name;
  private String configKey;
  private String templateUuid;
  private String language;

  public ActiveRuleDto() {
    // nothing to do here
  }

  public ActiveRuleDto setKey(ActiveRuleKey key) {
    this.repository = key.getRuleKey().repository();
    this.ruleField = key.getRuleKey().rule();
    this.profileUuid = key.getRuleProfileUuid();
    return this;
  }

  public ActiveRuleKey getKey() {
    return new ActiveRuleKey(profileUuid, RuleKey.of(repository, ruleField));
  }

  public RuleKey getRuleKey() {
    return RuleKey.of(repository, ruleField);
  }

  public String getUuid() {
    return uuid;
  }

  public ActiveRuleDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getProfileUuid() {
    return profileUuid;
  }

  public ActiveRuleDto setProfileUuid(String profileUuid) {
    this.profileUuid = profileUuid;
    return this;
  }

  public String getRuleUuid() {
    return ruleUuid;
  }

  public ActiveRuleDto setRuleUuid(String ruleUuid) {
    this.ruleUuid = ruleUuid;
    return this;
  }

  public Integer getSeverity() {
    return severity;
  }

  public String getSeverityString() {
    return SeverityUtil.getSeverityFromOrdinal(severity);
  }

  public ActiveRuleDto setSeverity(Integer severity) {
    this.severity = severity;
    return this;
  }

  public ActiveRuleDto setSeverity(String severity) {
    this.severity = SeverityUtil.getOrdinalFromSeverity(severity);
    return this;
  }

  @CheckForNull
  public String getInheritance() {
    return inheritance;
  }

  public ActiveRuleDto setInheritance(@Nullable String inheritance) {
    this.inheritance = inheritance;
    return this;
  }

  public boolean isInherited() {
    return StringUtils.equals(INHERITED, inheritance);
  }

  public boolean doesOverride() {
    return StringUtils.equals(OVERRIDES, inheritance);
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public ActiveRuleDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public ActiveRuleDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public String getSecurityStandards() {
    return securityStandards;
  }

  public ActiveRuleDto setSecurityStandards(String securityStandards) {
    this.securityStandards = securityStandards;
    return this;
  }

  public Map<SoftwareQuality, Severity> getImpacts() {
    return impacts != null ? GSON.fromJson(impacts, TYPE) : Map.of();
  }

  public String getImpactsString() {
    return this.impacts;
  }

  public ActiveRuleDto setImpactsString(@Nullable String impacts) {
    this.impacts = impacts;
    return this;
  }

  public ActiveRuleDto setImpacts(Map<SoftwareQuality, Severity> impacts) {
    this.impacts = !impacts.isEmpty() ? GSON.toJson(impacts, TYPE) : null;
    return this;
  }

  public boolean isExternal() {
    return this.isExternal;
  }

  public ActiveRuleDto setIsExternal(boolean isExternal) {
    this.isExternal = isExternal;
    return this;
  }

  public boolean isPrioritizedRule() {
    return prioritizedRule;
  }

  public ActiveRuleDto setPrioritizedRule(boolean prioritizedRule) {
    this.prioritizedRule = prioritizedRule;
    return this;
  }

  public String getName() {
    return name;
  }

  public ActiveRuleDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getConfigKey() {
    return configKey;
  }

  public ActiveRuleDto setConfigKey(String configKey) {
    this.configKey = configKey;
    return this;
  }

  @CheckForNull
  public String getTemplateUuid() {
    return templateUuid;
  }

  public ActiveRuleDto setTemplateUuid(@Nullable String templateUuid) {
    this.templateUuid = templateUuid;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public ActiveRuleDto setLanguage(String language) {
    this.language = language;
    return this;
  }

  public static ActiveRuleDto createFor(QProfileDto profile, RuleDto ruleDto) {
    requireNonNull(profile.getRulesProfileUuid(), "Profile is not persisted");
    requireNonNull(ruleDto.getUuid(), "Rule is not persisted");
    ActiveRuleDto dto = new ActiveRuleDto();
    dto.setProfileUuid(profile.getRulesProfileUuid());
    dto.setRuleUuid(ruleDto.getUuid());
    dto.setKey(ActiveRuleKey.of(profile, ruleDto.getKey()));
    dto.setName(ruleDto.getName());
    dto.setConfigKey(ruleDto.getConfigKey());
    dto.setTemplateUuid(ruleDto.getTemplateUuid());
    dto.setLanguage(ruleDto.getLanguage());
    dto.setImpacts(ruleDto.getDefaultImpactsMap());
    return dto;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }

}
