/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleStatus;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleParamDto;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.ws.WsUtils.checkRequest;

class RuleActivatorContext {

  private final QProfileDto profile;
  private final RulesProfileDto rulesProfile;
  private final Date initDate = new Date();
  private RuleDefinitionDto rule;
  private final Map<String, RuleParamDto> ruleParams = new HashMap<>();
  private ActiveRuleDto activeRule;
  private ActiveRuleDto parentActiveRule;
  private final Map<String, ActiveRuleParamDto> activeRuleParams = new HashMap<>();
  private final Map<String, ActiveRuleParamDto> parentActiveRuleParams = new HashMap<>();
  private final boolean isCascade;

  RuleActivatorContext(QProfileDto profile, boolean isCascade) {
    this.profile = profile;
    this.rulesProfile = RulesProfileDto.from(profile);
    this.isCascade = isCascade;
  }

  RuleActivatorContext(RulesProfileDto rulesProfile) {
    checkArgument(rulesProfile.isBuiltIn(), "Rules profile must be a built-in profile: " + rulesProfile.getKee());
    this.profile = null;
    this.rulesProfile = rulesProfile;
    this.isCascade = false;
  }

  @CheckForNull
  QProfileDto getProfile() {
    return profile;
  }

  RulesProfileDto getRulesProfile() {
    return rulesProfile;
  }

  boolean isBuiltIn() {
    return profile == null;
  }

  boolean isCascade() {
    return isCascade;
  }

  ActiveRuleKey activeRuleKey() {
    return ActiveRuleKey.of(rulesProfile, rule.getKey());
  }

  RuleDefinitionDto getRule() {
    return rule;
  }

  RuleActivatorContext setRule(RuleDefinitionDto rule) {
    this.rule = rule;
    return this;
  }



  Date getInitDate() {
    return initDate;
  }

  Map<String, RuleParamDto> ruleParamsByKeys() {
    return ruleParams;
  }

  Collection<RuleParamDto> ruleParams() {
    return ruleParams.values();
  }

  RuleActivatorContext setRuleParams(Collection<RuleParamDto> ruleParams) {
    this.ruleParams.clear();
    for (RuleParamDto ruleParam : ruleParams) {
      this.ruleParams.put(ruleParam.getName(), ruleParam);
    }
    return this;
  }

  @CheckForNull
  ActiveRuleDto activeRule() {
    return activeRule;
  }

  RuleActivatorContext setActiveRule(@Nullable ActiveRuleDto a) {
    this.activeRule = a;
    return this;
  }

  @CheckForNull
  ActiveRuleDto parentActiveRule() {
    return parentActiveRule;
  }

  RuleActivatorContext setParentActiveRule(@Nullable ActiveRuleDto a) {
    this.parentActiveRule = a;
    return this;
  }

  @CheckForNull
  String requestParamValue(RuleActivation request, String key) {
    if (rule.isCustomRule()) {
      return null;
    }
    return request.getParameter(key);
  }

  boolean hasRequestParamValue(RuleActivation request, String key) {
    return request.hasParameter(key);
  }

  @CheckForNull
  String currentParamValue(String key) {
    ActiveRuleParamDto param = activeRuleParams.get(key);
    return param != null ? param.getValue() : null;
  }

  @CheckForNull
  String parentParamValue(String key) {
    ActiveRuleParamDto param = parentActiveRuleParams.get(key);
    return param != null ? param.getValue() : null;
  }

  @CheckForNull
  String defaultParamValue(String key) {
    RuleParamDto param = ruleParams.get(key);
    return param != null ? param.getDefaultValue() : null;
  }

  @CheckForNull
  String currentSeverity() {
    return activeRule != null ? activeRule.getSeverityString() : null;
  }

  @CheckForNull
  String parentSeverity() {
    return parentActiveRule != null ? parentActiveRule.getSeverityString() : null;
  }

  @CheckForNull
  String defaultSeverity() {
    return rule.getSeverityString();
  }

  Map<String, ActiveRuleParamDto> activeRuleParamsAsMap() {
    return activeRuleParams;
  }

  Map<String, String> activeRuleParamsAsStringMap() {
    Map<String, String> params = Maps.newHashMap();
    for (Map.Entry<String, ActiveRuleParamDto> param : activeRuleParams.entrySet()) {
      params.put(param.getKey(), param.getValue().getValue());
    }
    return params;
  }

  RuleActivatorContext setActiveRuleParams(@Nullable Collection<ActiveRuleParamDto> a) {
    activeRuleParams.clear();
    if (a != null) {
      for (ActiveRuleParamDto ar : a) {
        this.activeRuleParams.put(ar.getKey(), ar);
      }
    }
    return this;
  }

  RuleActivatorContext setParentActiveRuleParams(@Nullable Collection<ActiveRuleParamDto> a) {
    parentActiveRuleParams.clear();
    if (a != null) {
      for (ActiveRuleParamDto ar : a) {
        this.parentActiveRuleParams.put(ar.getKey(), ar);
      }
    }
    return this;
  }

  /**
   * True if trying to override an inherited rule but with exactly the same values
   */
  boolean isSameAsParent(ActiveRuleChange change) {
    if (parentActiveRule == null) {
      return false;
    }
    if (!StringUtils.equals(change.getSeverity(), parentActiveRule.getSeverityString())) {
      return false;
    }
    for (Map.Entry<String, String> entry : change.getParameters().entrySet()) {
      if (entry.getValue() != null && !entry.getValue().equals(parentParamValue(entry.getKey()))) {
        return false;
      }
    }
    return true;
  }

  boolean isSame(ActiveRuleChange change) {
    ActiveRule.Inheritance inheritance = change.getInheritance();
    if (inheritance != null && !inheritance.name().equals(activeRule.getInheritance())) {
      return false;
    }
    String severity = change.getSeverity();
    if (severity != null && !severity.equals(activeRule.getSeverityString())) {
      return false;
    }
    for (Map.Entry<String, String> changeParam : change.getParameters().entrySet()) {
      ActiveRuleParamDto param = activeRuleParams.get(changeParam.getKey());
      if (changeParam.getValue() == null && param != null && param.getValue() != null) {
        return false;
      }
      if (changeParam.getValue() != null && (param == null || !StringUtils.equals(changeParam.getValue(), param.getValue()))) {
        return false;
      }
    }
    return true;
  }

  void verifyForActivation() {
    checkRequest(RuleStatus.REMOVED != rule.getStatus(), "Rule was removed: %s", rule.getKey());
    checkRequest(!rule.isTemplate(), "Rule template can't be activated on a Quality profile: %s", rule.getKey());
    checkRequest(rulesProfile.getLanguage().equals(rule.getLanguage()), "Rule %s and profile %s have different languages", rule.getKey(), profile != null ? profile.getKee() : rulesProfile.getKee());
  }
}
