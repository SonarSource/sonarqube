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
package org.sonar.batch.protocol.input;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ActiveRule {
  private final String repositoryKey, ruleKey, templateRuleKey;
  private final String name, severity, internalKey, language;
  private final Map<String, String> params = new HashMap<String, String>();

  public ActiveRule(String repositoryKey, String ruleKey, @Nullable String templateRuleKey, String name, @Nullable String severity,
    @Nullable String internalKey, @Nullable String language) {
    this.repositoryKey = repositoryKey;
    this.ruleKey = ruleKey;
    this.templateRuleKey = templateRuleKey;
    this.name = name;
    this.severity = severity;
    this.internalKey = internalKey;
    this.language = language;
  }

  public String repositoryKey() {
    return repositoryKey;
  }

  public String ruleKey() {
    return ruleKey;
  }

  @CheckForNull
  public String templateRuleKey() {
    return templateRuleKey;
  }

  public String name() {
    return name;
  }

  /**
   * Is null on manual rules
   */
  @CheckForNull
  public String severity() {
    return severity;
  }

  /**
   * Is null on manual rules
   */
  @CheckForNull
  public String language() {
    return language;
  }

  @CheckForNull
  public String param(String key) {
    return params.get(key);
  }

  public ActiveRule addParam(String key, String value) {
    params.put(key, value);
    return this;
  }

  public Map<String, String> params() {
    return params;
  }

  @CheckForNull
  public String internalKey() {
    return internalKey;
  }
}
