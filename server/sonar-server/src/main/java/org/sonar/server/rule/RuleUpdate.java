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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;

public class RuleUpdate {

  public static final String DEFAULT_DEBT_CHARACTERISTIC = "_default";

  private final RuleKey ruleKey;

  private boolean changeTags = false, changeMarkdownNote = false, changeDebtSubCharacteristic = false, changeDebtRemediationFunction = false,
    changeName = false, changeDescription = false, changeSeverity = false, changeStatus = false, changeParameters = false;
  private boolean isCustomRule, isManual;
  private Set<String> tags;
  private String markdownNote;
  private String debtSubCharacteristicKey;
  private DebtRemediationFunction debtRemediationFunction;

  private String name, markdownDescription, severity;
  private RuleStatus status;
  private final Map<String, String> parameters = Maps.newHashMap();

  private RuleUpdate(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
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
    this.markdownNote = (s == null ? null : StringUtils.defaultIfBlank(s, null));
    this.changeMarkdownNote = true;
    return this;
  }

  @CheckForNull
  public String getDebtSubCharacteristicKey() {
    return debtSubCharacteristicKey;
  }

  /**
   * Set to <code>null</code> or blank to force the characteristic "NONE". Set to value of {@link #DEFAULT_DEBT_CHARACTERISTIC}
   * to reset to default characteristic.
   */
  public RuleUpdate setDebtSubCharacteristic(@Nullable String c) {
    this.debtSubCharacteristicKey = (c == null ? null : StringUtils.defaultIfBlank(c, null));
    this.changeDebtSubCharacteristic = true;
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
    checkCustomOrManualRule();
    this.name = name;
    this.changeName = true;
    return this;
  }

  @CheckForNull
  public String getMarkdownDescription() {
    return markdownDescription;
  }

  public RuleUpdate setMarkdownDescription(@Nullable String markdownDescription) {
    checkCustomOrManualRule();
    this.markdownDescription = markdownDescription;
    this.changeDescription = true;
    return this;
  }

  @CheckForNull
  public String getSeverity() {
    return severity;
  }

  public RuleUpdate setSeverity(@Nullable String severity) {
    checkCustomOrManualRule();
    this.severity = severity;
    this.changeSeverity = true;
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
    return isCustomRule;
  }

  public boolean isChangeTags() {
    return changeTags;
  }

  public boolean isChangeMarkdownNote() {
    return changeMarkdownNote;
  }

  public boolean isChangeDebtSubCharacteristic() {
    return changeDebtSubCharacteristic;
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

  public boolean isChangeStatus() {
    return changeStatus;
  }

  public boolean isChangeParameters() {
    return changeParameters;
  }

  public boolean isEmpty() {
    return !changeMarkdownNote && !changeTags && !changeDebtSubCharacteristic && !changeDebtRemediationFunction && isCustomRuleFieldsEmpty();
  }

  private boolean isCustomRuleFieldsEmpty(){
    return !changeName && !changeDescription && !changeSeverity && !changeStatus && !changeParameters;
  }

  private void checkCustomRule(){
    if (!isCustomRule) {
      throw new IllegalStateException("Not a custom rule");
    }
  }

  private void checkCustomOrManualRule(){
    if (!isCustomRule && !isManual) {
      throw new IllegalStateException("Not a custom or a manual rule");
    }
  }

  /**
   * Use to update a rule provided by a plugin (name, description, severity, status and parameters cannot by changed)
   */
  public static RuleUpdate createForPluginRule(RuleKey ruleKey) {
    RuleUpdate ruleUpdate = new RuleUpdate(ruleKey);
    ruleUpdate.isCustomRule = false;
    ruleUpdate.isManual = false;
    return ruleUpdate;
  }

  /**
   * Use to update a custom rule
   */
  public static RuleUpdate createForCustomRule(RuleKey ruleKey) {
    RuleUpdate ruleUpdate = new RuleUpdate(ruleKey);
    ruleUpdate.isCustomRule = true;
    ruleUpdate.isManual = false;
    return ruleUpdate;
  }

  /**
   * Use to update a manual rule (status and parameters cannot by changed)
   */
  public static RuleUpdate createForManualRule(RuleKey ruleKey) {
    RuleUpdate ruleUpdate = new RuleUpdate(ruleKey);
    ruleUpdate.isManual = true;
    ruleUpdate.isCustomRule = false;
    return ruleUpdate;
  }

}
