/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;

public class RuleForIndexingDto {

  private Integer id;
  private String repository;
  private String pluginRuleKey;
  private String name;
  private String description;
  private RuleDto.Format descriptionFormat;
  private Integer severity;
  private RuleStatus status;
  private boolean isTemplate;
  private String systemTags;
  private String templateRuleKey;
  private String templateRepository;
  private String internalKey;
  private String language;
  private int type;
  private long createdAt;
  private long updatedAt;

  private static final Splitter TAGS_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  public Integer getId() {
    return id;
  }

  public String getRepository() {
    return repository;
  }

  public String getPluginRuleKey() {
    return pluginRuleKey;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public RuleDto.Format getDescriptionFormat() {
    return descriptionFormat;
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

  public String getSystemTags() {
    return systemTags;
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

  public long getCreatedAt() {
    return createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public RuleType getTypeAsRuleType() {
    return RuleType.valueOf(type);
  }

  public String getSeverityAsString() {
    return severity != null ? SeverityUtil.getSeverityFromOrdinal(severity) : null;
  }

  public RuleKey getRuleKey() {
    return RuleKey.of(repository, pluginRuleKey);
  }

  public Set<String> getSystemTagsAsSet() {
    return ImmutableSet.copyOf(TAGS_SPLITTER.split(systemTags == null ? "" : systemTags));
  }
}
