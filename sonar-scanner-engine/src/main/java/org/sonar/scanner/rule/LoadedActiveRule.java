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
package org.sonar.scanner.rule;

import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;

public class LoadedActiveRule {
  private RuleKey ruleKey;
  private String severity;
  private Map<SoftwareQuality, Severity> impacts;
  private String name;
  private String language;
  private Map<String, String> params;
  private long createdAt;
  private long updatedAt;
  private String templateRuleKey;
  private String internalKey;
  private Set<RuleKey> deprecatedKeys;

  public LoadedActiveRule() {
    // nothing to do here
  }

  public RuleKey getRuleKey() {
    return ruleKey;
  }

  public void setRuleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public Map<SoftwareQuality, Severity> getImpacts() {
    return impacts;
  }

  public void setImpacts(Map<SoftwareQuality, Severity> impacts) {
    this.impacts = impacts;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public void setParams(Map<String, String> params) {
    this.params = params;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }

  @CheckForNull
  public String getTemplateRuleKey() {
    return templateRuleKey;
  }

  public void setTemplateRuleKey(@Nullable String templateRuleKey) {
    this.templateRuleKey = templateRuleKey;
  }

  public String getInternalKey() {
    return internalKey;
  }

  public void setInternalKey(String internalKey) {
    this.internalKey = internalKey;
  }

  public Set<RuleKey> getDeprecatedKeys() {
    return deprecatedKeys;
  }

  public void setDeprecatedKeys(Set<RuleKey> deprecatedKeys) {
    this.deprecatedKeys = deprecatedKeys;
  }
}
