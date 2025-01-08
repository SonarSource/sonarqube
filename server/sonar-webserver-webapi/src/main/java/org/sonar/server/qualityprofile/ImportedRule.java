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
package org.sonar.server.qualityprofile;

import java.util.Map;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;

class ImportedRule {
  private String key = null;

  private String repository = null;

  private String template = null;
  private String name = null;
  private String type = null;
  private String severity = null;
  private Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts = Map.of();
  private Boolean prioritizedRule = false;
  private String description = null;
  private String cleanCodeAttribute = null;
  private Map<String, String> parameters = null;

  public Map<String, String> getParameters() {
    return parameters;
  }

  public RuleKey getRuleKey() {
    return RuleKey.of(repository, key);
  }

  public RuleKey getTemplateKey() {
    return RuleKey.of(repository, template);
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getSeverity() {
    return severity;
  }

  public Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> getImpacts() {
    return impacts;
  }

  public Boolean getPrioritizedRule() {
    return prioritizedRule;
  }

  public String getDescription() {
    return description;
  }

  public String getCleanCodeAttribute() {
    return cleanCodeAttribute;
  }

  ImportedRule setType(String type) {
    this.type = type;
    return this;
  }

  ImportedRule setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  ImportedRule setImpacts(Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts) {
    this.impacts = impacts;
    return this;
  }

  public ImportedRule setPrioritizedRule(Boolean prioritizedRule) {
    this.prioritizedRule = prioritizedRule;
    return this;
  }

  ImportedRule setDescription(String description) {
    this.description = description;
    return this;
  }

  public void setCleanCodeAttribute(String cleanCodeAttribute) {
    this.cleanCodeAttribute = cleanCodeAttribute;
  }

  ImportedRule setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
    return this;
  }

  ImportedRule setName(String name) {
    this.name = name;
    return this;
  }

  boolean isCustomRule() {
    return template != null;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public void setKey(String key) {
    this.key = key;
  }
}
