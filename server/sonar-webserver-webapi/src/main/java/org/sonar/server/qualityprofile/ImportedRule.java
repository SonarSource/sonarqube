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
package org.sonar.server.qualityprofile;

import java.util.Map;
import org.sonar.api.rule.RuleKey;

class ImportedRule {
  private RuleKey ruleKey = null;
  private RuleKey templateKey = null;
  private String name = null;
  private String type = null;
  private String severity = null;
  private String description = null;
  private Map<String, String> parameters = null;

  public Map<String, String> getParameters() {
    return parameters;
  }

  public RuleKey getRuleKey() {
    return ruleKey;
  }

  public RuleKey getTemplateKey() {
    return templateKey;
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

  public String getDescription() {
    return description;
  }

  ImportedRule setRuleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  ImportedRule setTemplateKey(RuleKey templateKey) {
    this.templateKey = templateKey;
    return this;
  }

  ImportedRule setType(String type) {
    this.type = type;
    return this;
  }

  ImportedRule setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  ImportedRule setDescription(String description) {
    this.description = description;
    return this;
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
    return templateKey != null;
  }
}
