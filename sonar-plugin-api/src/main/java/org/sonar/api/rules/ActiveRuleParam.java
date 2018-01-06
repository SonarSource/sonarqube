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

public class ActiveRuleParam implements Cloneable {

  private Integer id;
  private ActiveRule activeRule;
  private RuleParam ruleParam;
  private String paramKey;
  private String value;

  /**
   * @deprecated visibility should be decreased to protected or package
   */
  @Deprecated
  public ActiveRuleParam() {
  }

  /**
   * @deprecated visibility should be decreased to protected or package
   */
  @Deprecated
  public ActiveRuleParam(ActiveRule activeRule, RuleParam ruleParam, String value) {
    this.activeRule = activeRule;
    this.ruleParam = ruleParam;
    this.value = value;
    this.paramKey = ruleParam.getKey();
  }

  public Integer getId() {
    return id;
  }

  /**
   * @deprecated visibility should be decreased to protected or package
   */
  @Deprecated
  void setId(Integer id) {
    this.id = id;
  }

  public ActiveRule getActiveRule() {
    return activeRule;
  }

  /**
   * @deprecated visibility should be decreased to protected or package
   */
  @Deprecated
  public void setActiveRule(ActiveRule activeRule) {
    this.activeRule = activeRule;
  }

  public RuleParam getRuleParam() {
    return ruleParam;
  }

  /**
   * @deprecated visibility should be decreased to protected or package
   */
  @Deprecated
  public void setRuleParam(RuleParam ruleParam) {
    this.ruleParam = ruleParam;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getParamKey() {
    return paramKey;
  }

  public void setParamKey(String paramKey) {
    this.paramKey = paramKey;
  }


  public String getKey() {
    return ruleParam.getKey();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ActiveRuleParam)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    ActiveRuleParam other = (ActiveRuleParam) obj;
    return other.getKey().equals(getKey());
  }

  @Override
  public int hashCode() {
    return getKey().hashCode();
  }

  @Override
  public Object clone() {
    return new ActiveRuleParam(getActiveRule(), getRuleParam(), getValue());
  }

}
