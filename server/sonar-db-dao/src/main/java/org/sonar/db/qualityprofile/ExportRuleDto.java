/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.db.rule.SeverityUtil;

public class ExportRuleDto {
  private String activeRuleUuid = null;
  private String cleanCodeAttribute = null;
  private String description = null;
  private String repository = null;
  private String rule = null;
  private String name = null;
  private String extendedDescription = null;
  private String template = null;
  private Integer severity = null;
  private String impacts = null;
  private Integer type = null;
  private Boolean prioritizedRule;
  private Set<String> tags = new HashSet<>();

  private List<ExportRuleParamDto> params = null;

  public boolean isCustomRule() {
    return template != null;
  }

  public String getActiveRuleUuid() {
    return activeRuleUuid;
  }

  public RuleKey getRuleKey() {
    return RuleKey.of(repository, rule);
  }

  public RuleKey getTemplateRuleKey() {
    return RuleKey.of(repository, template);
  }

  public String getSeverityString() {
    return SeverityUtil.getSeverityFromOrdinal(severity);
  }

  public String getExtendedDescription() {
    return extendedDescription;
  }

  public RuleType getRuleType() {
    return RuleType.valueOf(type);
  }

  @CheckForNull
  public Boolean getPrioritizedRule() {
    return prioritizedRule;
  }

  public String getImpacts() {
    return impacts;
  }

  public Set<String> getTags() {
    return tags;
  }

  public String getName() {
    return name;
  }

  public String getDescriptionOrThrow() {
    return Objects.requireNonNull(description, "description is expected to be set but it is null");
  }

  public String getCleanCodeAttribute() {
    return cleanCodeAttribute;
  }

  public List<ExportRuleParamDto> getParams() {
    if (params == null) {
      params = new LinkedList<>();
    }
    return params;
  }

  void setParams(List<ExportRuleParamDto> params) {
    this.params = params;
  }

}
