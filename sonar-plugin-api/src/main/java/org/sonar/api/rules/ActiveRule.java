/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.rules;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.profiles.RulesProfile;

public class ActiveRule implements Cloneable {

  public static final String INHERITED = "INHERITED";
  public static final String OVERRIDES = "OVERRIDES";

  private Integer id;
  private Rule rule;
  private RulePriority severity;
  private RulePriority overriddenSeverity;
  private RulesProfile rulesProfile;
  private List<ActiveRuleParam> activeRuleParams = new ArrayList<>();
  private String inheritance;

  /**
   * @deprecated visibility should be reduced to protected or package
   */
  @Deprecated
  public ActiveRule() {
  }

  /**
   * @deprecated visibility should be reduced to protected or package
   */
  @Deprecated
  public ActiveRule(RulesProfile profile, Rule rule, RulePriority severity) {
    this.rule = rule;
    this.overriddenSeverity = severity;
    if (severity == null && rule != null) {
      this.severity = rule.getSeverity();
    } else {
      this.severity = severity;
    }

    this.rulesProfile = profile;
  }

  public Integer getId() {
    return id;
  }

  /**
   * For internal use only.
   *
   * @since 2.5
   */
  public String getInheritance() {
    return inheritance;
  }

  /**
   * For internal use only.
   *
   * @since 2.5
   */
  public void setInheritance(String s) {
    this.inheritance = s;
  }

  public boolean isInherited() {
    return StringUtils.equals(INHERITED, inheritance);
  }

  public boolean doesOverride() {
    return StringUtils.equals(OVERRIDES, inheritance);
  }

  /**
   * @deprecated visibility should be decreased to protected or package
   */
  @Deprecated
  public void setId(Integer id) {
    this.id = id;
  }

  public Rule getRule() {
    return rule;
  }

  /**
   * @deprecated visibility should be reduced to protected or package
   */
  @Deprecated
  public void setRule(Rule rule) {
    this.rule = rule;
  }

  /**
   * @since 2.5
   */
  public RulePriority getSeverity() {
    return severity;
  }

  /**
   * For internal use
   * @since 6.6
   * @deprecated
   */
  @Deprecated
  public RulePriority getOverriddenSeverity() {
    return overriddenSeverity;
  }

  /**
   * @since 2.5
   */
  public void setSeverity(RulePriority severity) {
    this.severity = severity;
  }

  /**
   * @deprecated since 2.5 use {@link #getSeverity()} instead. See http://jira.sonarsource.com/browse/SONAR-1829
   */
  @Deprecated
  public RulePriority getPriority() {
    return severity;
  }

  /**
   * @deprecated since 2.5 use {@link #setSeverity(RulePriority)} instead. See http://jira.sonarsource.com/browse/SONAR-1829
   */
  @Deprecated
  public void setPriority(RulePriority priority) {
    this.severity = priority;
  }

  public RulesProfile getRulesProfile() {
    return rulesProfile;
  }

  /**
   * @deprecated visibility should be reduced to protected or package
   */
  @Deprecated
  public void setRulesProfile(RulesProfile rulesProfile) {
    this.rulesProfile = rulesProfile;
  }

  public List<ActiveRuleParam> getActiveRuleParams() {
    return activeRuleParams;
  }

  /**
   * @deprecated use setParameter()
   */
  @Deprecated
  public void setActiveRuleParams(List<ActiveRuleParam> params) {
    this.activeRuleParams = params;
  }

  public ActiveRule setParameter(String key, String value) {
    RuleParam ruleParameter = rule.getParam(key);
    if (ruleParameter != null) {
      activeRuleParams.add(new ActiveRuleParam(this, ruleParameter, value));
    }
    return this;
  }

  public String getParameter(String key) {
    if (activeRuleParams != null) {
      for (ActiveRuleParam param : activeRuleParams) {
        if (StringUtils.equals(key, param.getKey())) {
          return param.getValue();
        }
      }
    }
    return null;
  }

  /**
   * @deprecated since 2.3 use {@link #getRepositoryKey()} instead
   */
  @Deprecated
  public String getPluginName() {
    return rule.getRepositoryKey();
  }

  public String getRepositoryKey() {
    return rule.getRepositoryKey();
  }

  /**
   * @return the config key the active rule belongs to
   */
  public String getConfigKey() {
    return rule.getConfigKey();
  }

  /**
   * @return the key of the active rule
   */
  public String getRuleKey() {
    return rule.getKey();
  }

  /**
   * @since 4.2
   * @deprecated in 4.4. Feature dropped.
   */
  @CheckForNull
  @Deprecated
  public String getNoteData() {
    return null;
  }

  /**
   * @since 4.2
   * @deprecated in 4.4. Feature dropped.
   */
  @CheckForNull
  @Deprecated
  public String getNoteUserLogin() {
    return null;
  }

  /**
   * @since 4.2
   * @deprecated in 4.4. Feature dropped.
   */
  @CheckForNull
  @Deprecated
  public Date getNoteCreatedAt() {
    return null;
  }

  /**
   * @since 4.2
   * @deprecated in 4.4. Feature dropped.
   */
  @CheckForNull
  @Deprecated
  public Date getNoteUpdatedAt() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ActiveRule that = (ActiveRule) o;

    if (!rule.equals(that.rule)) {
      return false;
    }
    return !((rulesProfile != null) ? !rulesProfile.equals(that.rulesProfile) : (that.rulesProfile != null));

  }

  @Override
  public int hashCode() {
    int result = rule.hashCode();
    result = 31 * result + (rulesProfile != null ? rulesProfile.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("id", getId()).append("rule", rule).append("priority", severity)
      .append("params", activeRuleParams).toString();
  }

  @Override
  public Object clone() {
    final ActiveRule clone = new ActiveRule(getRulesProfile(), getRule(), getSeverity());
    clone.setInheritance(getInheritance());
    if (activeRuleParams != null && !activeRuleParams.isEmpty()) {
      clone.setActiveRuleParams(activeRuleParams
        .stream()
        .map(input -> {
          ActiveRuleParam activeRuleParamClone = (ActiveRuleParam) input.clone();
          activeRuleParamClone.setActiveRule(clone);
          return activeRuleParamClone;
        })
        .collect(Collectors.toList()));
    }
    return clone;
  }

  /**
   * @since 2.6
   */
  public boolean isEnabled() {
    return getRule() != null && getRule().isEnabled();
  }
}
