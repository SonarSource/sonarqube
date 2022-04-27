/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;

import static org.sonar.db.rule.RuleDescriptionSectionDto.DEFAULT_KEY;

public class RuleForIndexingDto {

  private String uuid;
  private String repository;
  private String pluginRuleKey;
  private String name;
  private RuleDto.Format descriptionFormat;
  private Integer severity;
  private RuleStatus status;
  private boolean isTemplate;
  private String systemTags;
  private String tags;
  private String securityStandards;
  private String templateRuleKey;
  private String templateRepository;
  private String internalKey;
  private String language;
  private boolean isExternal;
  private int type;
  private long createdAt;
  private long updatedAt;

  private Set<RuleDescriptionSectionDto> ruleDescriptionSectionsDtos = new HashSet<>();

  public RuleForIndexingDto() {
    // nothing to do here
  }

  public String getUuid() {
    return uuid;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public String getPluginRuleKey() {
    return pluginRuleKey;
  }

  public void setPluginRuleKey(String pluginRuleKey) {
    this.pluginRuleKey = pluginRuleKey;
  }

  public String getName() {
    return name;
  }

  public RuleDto.Format getDescriptionFormat() {
    return descriptionFormat;
  }

  public void setDescriptionFormat(RuleDto.Format descriptionFormat) {
    this.descriptionFormat = descriptionFormat;
  }

  public Integer getSeverity() {
    return severity;
  }

  public RuleStatus getStatus() {
    return status;
  }

  public boolean isTemplate() {
    return isTemplate;
  }

  public Set<String> getSystemTags() {
    return RuleDefinitionDto.deserializeTagsString(systemTags);
  }

  public Set<String> getTags() {
    return RuleDefinitionDto.deserializeTagsString(tags);
  }

  public Set<String> getSecurityStandards() {
    return RuleDefinitionDto.deserializeSecurityStandardsString(securityStandards);
  }

  public String getTemplateRuleKey() {
    return templateRuleKey;
  }

  public String getTemplateRepository() {
    return templateRepository;
  }

  public String getInternalKey() {
    return internalKey;
  }

  public String getLanguage() {
    return language;
  }

  public int getType() {
    return type;
  }

  public boolean isExternal() {
    return isExternal;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  @CheckForNull
  public RuleType getTypeAsRuleType() {
    return RuleType.valueOfNullable(type);
  }

  public String getSeverityAsString() {
    return severity != null ? SeverityUtil.getSeverityFromOrdinal(severity) : null;
  }

  public RuleKey getRuleKey() {
    return RuleKey.of(repository, pluginRuleKey);
  }

  public Set<RuleDescriptionSectionDto> getRuleDescriptionSectionsDtos() {
    return ruleDescriptionSectionsDtos;
  }

  public void setRuleDescriptionSectionsDtos(Set<RuleDescriptionSectionDto> ruleDescriptionSectionsDtos) {
    this.ruleDescriptionSectionsDtos = ruleDescriptionSectionsDtos;
  }

  private Optional<RuleDescriptionSectionDto> findExistingSectionWithSameKey(String ruleDescriptionSectionKey) {
    return ruleDescriptionSectionsDtos.stream().filter(section -> section.getKey().equals(ruleDescriptionSectionKey)).findAny();
  }

  @CheckForNull
  public RuleDescriptionSectionDto getDefaultRuleDescriptionSectionDto() {
    return findExistingSectionWithSameKey(DEFAULT_KEY).orElse(null);
  }
}
