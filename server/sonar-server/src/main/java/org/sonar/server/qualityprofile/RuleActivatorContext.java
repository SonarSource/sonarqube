/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleStatus;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.exceptions.BadRequestException;

class RuleActivatorContext {

  private final Date initDate = new Date();
  private RuleDto rule;
  private final Map<String, RuleParamDto> ruleParams = Maps.newHashMap();
  private QualityProfileDto profile;
  private ActiveRuleDto activeRule;
  private ActiveRuleDto parentActiveRule;
  private final Map<String, ActiveRuleParamDto> activeRuleParams = Maps.newHashMap();
  private final Map<String, ActiveRuleParamDto> parentActiveRuleParams = Maps.newHashMap();

  RuleActivatorContext() {
  }

  ActiveRuleKey activeRuleKey() {
    return ActiveRuleKey.of(profile.getKee(), rule.getKey());
  }

  RuleDto rule() {
    return rule;
  }

  RuleActivatorContext setRule(RuleDto rule) {
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

  QualityProfileDto profile() {
    return profile;
  }

  RuleActivatorContext setProfile(QualityProfileDto profile) {
    this.profile = profile;
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
    if (rule.getTemplateId() != null) {
      return null;
    }
    return request.getParameters().get(key);
  }

  boolean hasRequestParamValue(RuleActivation request, String key) {
    return request.getParameters().containsKey(key);
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
      if (changeParam.getValue()==null && param != null && param.getValue()!=null) {
        return false;
      }
      if (changeParam.getValue()!=null && (param == null || !StringUtils.equals(changeParam.getValue(), param.getValue()))) {
        return false;
      }
    }
    return true;
  }

  void verifyForActivation() {
    if (RuleStatus.REMOVED == rule.getStatus()) {
      throw new BadRequestException("Rule was removed: " + rule.getKey());
    }
    if (rule.isTemplate()) {
      throw new BadRequestException("Rule template can't be activated on a Quality profile: " + rule.getKey());
    }
    if (!profile.getLanguage().equals(rule.getLanguage())) {
      throw new BadRequestException(String.format("Rule %s and profile %s have different languages", rule.getKey(), profile.getKey()));
    }

  }
}
