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
package org.sonar.server.rule;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.rule.RuleUpdate.RuleUpdateUseCase.CUSTOM_RULE;
import static org.sonar.server.rule.RuleUpdate.RuleUpdateUseCase.PLUGIN_RULE;

public class RuleUpdate {

  private final RuleKey ruleKey;

  private boolean changeTags = false;
  private boolean changeMarkdownNote = false;
  private boolean changeDebtRemediationFunction = false;
  private boolean changeName = false;
  private boolean changeDescription = false;
  private boolean changeSeverity = false;
  private boolean changeImpacts = false;
  private boolean changeStatus = false;
  private boolean changeParameters = false;
  private final RuleUpdateUseCase useCase;
  private Set<String> tags;
  private String markdownNote;
  private DebtRemediationFunction debtRemediationFunction;

  private String name;
  private String markdownDescription;
  private String severity;
  private final Map<SoftwareQuality, Severity> impactSeverities = new EnumMap<>(SoftwareQuality.class);
  private RuleStatus status;
  private final Map<String, String> parameters = new HashMap<>();

  private RuleUpdate(RuleKey ruleKey, RuleUpdateUseCase useCase) {
    this.ruleKey = ruleKey;
    this.useCase = useCase;
  }

  public RuleKey getRuleKey() {
    return ruleKey;
  }

  @CheckForNull
  public Set<String> getTags() {
    return tags;
  }

  /**
   * Set to <code>null</code> or empty set to remove existing tags.
   */
  public RuleUpdate setTags(@Nullable Set<String> tags) {
    this.tags = tags;
    this.changeTags = true;
    return this;
  }

  @CheckForNull
  public String getMarkdownNote() {
    return markdownNote;
  }

  /**
   * Set to <code>null</code> or blank to remove existing note.
   */
  public RuleUpdate setMarkdownNote(@Nullable String s) {
    this.markdownNote = s == null ? null : StringUtils.defaultIfBlank(s, null);
    this.changeMarkdownNote = true;
    return this;
  }

  @CheckForNull
  public DebtRemediationFunction getDebtRemediationFunction() {
    return debtRemediationFunction;
  }

  public RuleUpdate setDebtRemediationFunction(@Nullable DebtRemediationFunction fn) {
    this.debtRemediationFunction = fn;
    this.changeDebtRemediationFunction = true;
    return this;
  }

  @CheckForNull
  public String getName() {
    return name;
  }

  public RuleUpdate setName(@Nullable String name) {
    checkCustomRule();
    this.name = name;
    this.changeName = true;
    return this;
  }

  @CheckForNull
  public String getMarkdownDescription() {
    return markdownDescription;
  }

  public RuleUpdate setMarkdownDescription(@Nullable String markdownDescription) {
    checkCustomRule();
    this.markdownDescription = markdownDescription;
    this.changeDescription = true;
    return this;
  }

  @CheckForNull
  public String getSeverity() {
    return severity;
  }

  public RuleUpdate setSeverity(@Nullable String severity) {
    checkCustomRule();
    this.severity = severity;
    this.changeSeverity = true;
    return this;
  }

  public Map<SoftwareQuality, Severity> getImpactSeverities() {
    return impactSeverities;
  }

  /**
   * Impacts to be updated (only for custom rules)
   */
  public RuleUpdate setImpactSeverities(Map<SoftwareQuality, Severity> impactSeverities) {
    checkCustomRule();
    this.impactSeverities.clear();
    this.impactSeverities.putAll(impactSeverities);
    changeImpacts = true;
    return this;
  }

  @CheckForNull
  public RuleStatus getStatus() {
    return status;
  }

  public RuleUpdate setStatus(@Nullable RuleStatus status) {
    checkCustomRule();
    this.status = status;
    this.changeStatus = true;
    return this;
  }

  /**
   * Parameters to be updated (only for custom rules)
   */
  public RuleUpdate setParameters(Map<String, String> params) {
    checkCustomRule();
    this.parameters.clear();
    this.parameters.putAll(params);
    this.changeParameters = true;
    return this;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  @CheckForNull
  public String parameter(final String paramKey) {
    return parameters.get(paramKey);
  }

  boolean isCustomRule() {
    return useCase.isCustomRule;
  }

  public boolean isChangeTags() {
    return changeTags;
  }

  public boolean isChangeMarkdownNote() {
    return changeMarkdownNote;
  }

  public boolean isChangeDebtRemediationFunction() {
    return changeDebtRemediationFunction;
  }

  public boolean isChangeName() {
    return changeName;
  }

  public boolean isChangeDescription() {
    return changeDescription;
  }

  public boolean isChangeSeverity() {
    return changeSeverity;
  }

  public boolean isChangeImpacts() {
    return changeImpacts;
  }

  public boolean isChangeStatus() {
    return changeStatus;
  }

  public boolean isChangeParameters() {
    return changeParameters;
  }

  public boolean isEmpty() {
    return !changeMarkdownNote && !changeTags && !changeDebtRemediationFunction && isCustomRuleFieldsEmpty();
  }

  private boolean isCustomRuleFieldsEmpty() {
    return !changeName && !changeDescription && !changeSeverity && !changeStatus && !changeParameters && !changeImpacts;
  }

  private void checkCustomRule() {
    checkArgument(useCase == CUSTOM_RULE, "Not a custom rule");
  }

  /**
   * Use to update a rule provided by a plugin (name, description, severity, status and parameters cannot by changed)
   */
  public static RuleUpdate createForPluginRule(RuleKey ruleKey) {
    return new RuleUpdate(ruleKey, PLUGIN_RULE);
  }

  /**
   * Use to update a custom rule
   */
  public static RuleUpdate createForCustomRule(RuleKey ruleKey) {
    return new RuleUpdate(ruleKey, CUSTOM_RULE);
  }

  public enum RuleUpdateUseCase {
    PLUGIN_RULE(false),
    CUSTOM_RULE(true);

    public final boolean isCustomRule;

    RuleUpdateUseCase(boolean isCustomRule) {
      this.isCustomRule = isCustomRule;
    }
  }
}
