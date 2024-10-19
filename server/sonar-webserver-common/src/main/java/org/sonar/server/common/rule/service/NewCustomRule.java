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
package org.sonar.server.common.rule.service;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.server.common.rule.ReactivationException;

public class NewCustomRule {

  private RuleKey ruleKey;
  private RuleKey templateKey;
  private String name;
  private String markdownDescription;
  private String severity;
  private RuleStatus status;
  private RuleType type;
  private String organizationKey;
  private Map<String, String> parameters = new HashMap<>();
  private CleanCodeAttribute cleanCodeAttribute;
  private List<Impact> impacts;
  private boolean preventReactivation = false;

  private NewCustomRule() {
    // No direct call to constructor
  }

  public RuleKey ruleKey() {
    return ruleKey;
  }

  public RuleKey templateKey() {
    return templateKey;
  }

  @CheckForNull
  public String name() {
    return name;
  }

  public NewCustomRule setName(@Nullable String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String markdownDescription() {
    return markdownDescription;
  }

  public NewCustomRule setMarkdownDescription(@Nullable String markdownDescription) {
    this.markdownDescription = markdownDescription;
    return this;
  }

  @CheckForNull
  public String severity() {
    return severity;
  }

  @Deprecated(since = "10.4")
  public NewCustomRule setSeverity(@Nullable String severity) {
    this.severity = severity;
    return this;
  }

  @CheckForNull
  public RuleStatus status() {
    return status;
  }

  public NewCustomRule setStatus(@Nullable RuleStatus status) {
    this.status = status;
    return this;
  }

  @CheckForNull
  public RuleType type() {
    return type;
  }

  @Deprecated(since = "10.4")
  public NewCustomRule setType(@Nullable RuleType type) {
    this.type = type;
    return this;
  }

  @CheckForNull
  public String parameter(final String paramKey) {
    return parameters.get(paramKey);
  }

  public NewCustomRule setParameters(Map<String, String> params) {
    this.parameters = params;
    return this;
  }

  public CleanCodeAttribute getCleanCodeAttribute() {
    return cleanCodeAttribute;
  }

  public NewCustomRule setCleanCodeAttribute(@Nullable CleanCodeAttribute cleanCodeAttribute) {
    this.cleanCodeAttribute = cleanCodeAttribute;
    return this;
  }

  public List<Impact> getImpacts() {
    return impacts;
  }

  public NewCustomRule setImpacts(List<Impact> impacts) {
    this.impacts = impacts;
    return this;
  }

  public boolean isPreventReactivation() {
    return preventReactivation;
  }

  /**
   * When true, if the rule already exists in status REMOVED, an {@link ReactivationException} will be thrown
   */
  public NewCustomRule setPreventReactivation(boolean preventReactivation) {
    this.preventReactivation = preventReactivation;
    return this;
  }

  public String getOrganizationKey() {
    return organizationKey;
  }

  public NewCustomRule setOrganizationKey(String organizationKey) {
    this.organizationKey = organizationKey;
    return this;
  }

  public static NewCustomRule createForCustomRule(RuleKey customKey, RuleKey templateKey) {
    Preconditions.checkArgument(customKey != null, "Custom key should be set");
    Preconditions.checkArgument(templateKey != null, "Template key should be set");
    NewCustomRule newRule = new NewCustomRule();
    newRule.ruleKey = customKey;
    newRule.templateKey = templateKey;
    return newRule;
  }

  public record Impact(SoftwareQuality softwareQuality, Severity severity) {
  }
}
