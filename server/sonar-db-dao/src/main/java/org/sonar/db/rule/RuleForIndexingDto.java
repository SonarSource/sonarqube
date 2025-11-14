/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.core.rule.RuleType;
import org.sonar.db.issue.ImpactDto;

public class RuleForIndexingDto {

  private String uuid;
  private String repository;
  private String pluginRuleKey;
  private String name;
  private RuleDto.Format descriptionFormat;
  private Integer severity;
  private RuleStatus status;
  private boolean isTemplate;
  private Set<String> systemTags;
  private Set<String> tags;
  private Set<String> securityStandards;
  private String templateRuleKey;
  private String templateRepository;
  private String internalKey;
  private String language;
  private boolean isExternal;
  private boolean isAdHoc;
  private Integer adHocType;
  private int type;

  private long createdAt;
  private long updatedAt;
  private Set<RuleDescriptionSectionDto> ruleDescriptionSectionsDtos = new HashSet<>();

  private String cleanCodeAttributeCategory;
  private Set<ImpactDto> impacts = new HashSet<>();

  @VisibleForTesting
  public RuleForIndexingDto() {
    // nothing to do here
  }

  public static RuleForIndexingDto fromRuleDto(RuleDto r) {
    RuleForIndexingDto ruleForIndexingDto = new RuleForIndexingDto();
    ruleForIndexingDto.createdAt = r.getCreatedAt();
    ruleForIndexingDto.uuid = r.getUuid();
    ruleForIndexingDto.repository = r.getRepositoryKey();
    ruleForIndexingDto.pluginRuleKey = r.getRuleKey();
    ruleForIndexingDto.name = r.getName();
    ruleForIndexingDto.descriptionFormat = r.getDescriptionFormat();
    ruleForIndexingDto.severity = r.getSeverity();
    ruleForIndexingDto.status = r.getStatus();
    ruleForIndexingDto.isTemplate = r.isTemplate();
    ruleForIndexingDto.systemTags = Sets.newHashSet(r.getSystemTags());
    ruleForIndexingDto.tags = r.getTags() != null ? Sets.newHashSet(r.getTags()) : Collections.emptySet();
    ruleForIndexingDto.securityStandards = Sets.newHashSet(r.getSecurityStandards());
    ruleForIndexingDto.internalKey = r.getConfigKey();
    ruleForIndexingDto.language = r.getLanguage();
    ruleForIndexingDto.isExternal = r.isExternal();
    ruleForIndexingDto.isAdHoc = r.isAdHoc();
    ruleForIndexingDto.adHocType = r.getAdHocType();
    ruleForIndexingDto.type = r.getType();
    ruleForIndexingDto.createdAt = r.getCreatedAt();
    ruleForIndexingDto.updatedAt = r.getUpdatedAt();
    if (r.getRuleDescriptionSectionDtos() != null) {
      ruleForIndexingDto.setRuleDescriptionSectionsDtos(Sets.newHashSet(r.getRuleDescriptionSectionDtos()));
    }

    CleanCodeAttribute cleanCodeAttribute = r.getCleanCodeAttribute();
    if (cleanCodeAttribute != null) {
      ruleForIndexingDto.cleanCodeAttributeCategory = cleanCodeAttribute.getAttributeCategory().name();
    }
    ruleForIndexingDto.setImpacts(r.getDefaultImpacts());

    return ruleForIndexingDto;
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
    return Collections.unmodifiableSet(systemTags);
  }

  public Set<String> getTags() {
    return Collections.unmodifiableSet(tags);
  }

  public Set<String> getSecurityStandards() {
    return Collections.unmodifiableSet(securityStandards);
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

  public boolean isAdHoc() {
    return isAdHoc;
  }

  public Integer getAdHocType() {
    return adHocType;
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
    return Collections.unmodifiableSet(ruleDescriptionSectionsDtos);
  }

  public void setRuleDescriptionSectionsDtos(Set<RuleDescriptionSectionDto> ruleDescriptionSectionsDtos) {
    this.ruleDescriptionSectionsDtos = ruleDescriptionSectionsDtos;
  }

  public void setTemplateRuleKey(String templateRuleKey) {
    this.templateRuleKey = templateRuleKey;
  }

  public void setTemplateRepository(String templateRepository) {
    this.templateRepository = templateRepository;
  }

  public void setType(int type) {
    this.type = type;
  }

  @CheckForNull
  public String getCleanCodeAttributeCategory() {
    return cleanCodeAttributeCategory;
  }

  public void setCleanCodeAttributeCategory(String cleanCodeAttributeCategory) {
    this.cleanCodeAttributeCategory = cleanCodeAttributeCategory;
  }

  public Set<ImpactDto> getImpacts() {
    return impacts;
  }

  public void setImpacts(Set<ImpactDto> impacts) {
    this.impacts = impacts;
  }
}
