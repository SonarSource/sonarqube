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
package org.sonar.server.rule;

import com.google.common.collect.Maps;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Map;

public class NewRule {

  private boolean isTemplate;
  private RuleKey templateKey;
  private String name, htmlDescription, severity;
  private RuleStatus status;
  private final Map<String, String> params = Maps.newHashMap();

  public boolean isTemplate() {
    return isTemplate;
  }

  public void setIsTemplate(boolean template) {
    this.isTemplate = template;
  }

  @CheckForNull
  public RuleKey templateKey() {
    return templateKey;
  }

  /**
   * For the creation a custom rule
   */
  public NewRule setTemplateKey(@Nullable RuleKey templateKey) {
    this.templateKey = templateKey;
    return this;
  }

  @CheckForNull
  public String name() {
    return name;
  }

  public NewRule setName(@Nullable String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String htmlDescription() {
    return htmlDescription;
  }

  public NewRule setHtmlDescription(@Nullable String htmlDescription) {
    this.htmlDescription = htmlDescription;
    return this;
  }

  @CheckForNull
  public String severity() {
    return severity;
  }

  public NewRule setSeverity(@Nullable String severity) {
    this.severity = severity;
    return this;
  }

  @CheckForNull
  public RuleStatus status() {
    return status;
  }

  public NewRule setStatus(@Nullable RuleStatus status) {
    this.status = status;
    return this;
  }

  public Map<String, String> params() {
    return params;
  }

  @CheckForNull
  public String param(final String paramKey) {
    return params.get(paramKey);
  }

  public NewRule setParams(Map<String, String> params) {
    this.params.clear();
    this.params.putAll(params);
    return this;
  }
}
