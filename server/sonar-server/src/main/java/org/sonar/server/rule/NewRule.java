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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Map;

public class NewRule {

  private String ruleKey;
  private RuleKey templateKey;
  private String name;
  private String htmlDescription;
  private String markdownDescription;
  private String severity;
  private RuleStatus status;
  private final Map<String, String> parameters = Maps.newHashMap();

  private boolean isCustom;
  private boolean isManual;
  private boolean preventReactivation = false;

  private NewRule() {
    // No direct call to constructor
  }

  public String ruleKey() {
    return ruleKey;
  }

  @CheckForNull
  public RuleKey templateKey() {
    return templateKey;
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
  public String markdownDescription() {
    return markdownDescription;
  }

  public NewRule setMarkdownDescription(@Nullable String markdownDescription) {
    this.markdownDescription = markdownDescription;
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
    checkCustomRule();
    this.status = status;
    return this;
  }

  public Map<String, String> parameters() {
    return parameters;
  }

  @CheckForNull
  public String parameter(final String paramKey) {
    return parameters.get(paramKey);
  }

  public NewRule setParameters(Map<String, String> params) {
    checkCustomRule();
    this.parameters.clear();
    this.parameters.putAll(params);
    return this;
  }

  public boolean isPreventReactivation() {
    return preventReactivation;
  }

  /**
   * When true, if the rule already exists in status REMOVED, an {@link ReactivationException} will be thrown
   */
  public NewRule setPreventReactivation(boolean preventReactivation) {
    this.preventReactivation = preventReactivation;
    return this;
  }

  private void checkCustomRule(){
    if (!isCustom) {
      throw new IllegalStateException("Not a custom rule");
    }
  }

  public boolean isCustom() {
    return isCustom;
  }

  public boolean isManual() {
    return isManual;
  }

  public static NewRule createForCustomRule(String customKey, RuleKey templateKey) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(customKey), "Custom key should be set");
    Preconditions.checkArgument(templateKey != null, "Template key should be set");
    NewRule newRule = new NewRule();
    newRule.ruleKey = customKey;
    newRule.templateKey = templateKey;
    newRule.isCustom = true;
    return newRule;
  }

  public static NewRule createForManualRule(String manualKey) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(manualKey), "Manual key should be set");
    NewRule newRule = new NewRule();
    newRule.ruleKey = manualKey;
    newRule.isManual = true;
    return newRule;
  }

}
