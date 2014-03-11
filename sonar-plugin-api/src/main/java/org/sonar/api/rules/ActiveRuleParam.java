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
package org.sonar.api.rules;

import javax.persistence.*;

@Entity
@Table(name = "active_rule_parameters")
public class ActiveRuleParam implements Cloneable {

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "active_rule_id")
  private ActiveRule activeRule;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "rules_parameter_id")
  private RuleParam ruleParam;

  @Column(name = "rules_parameter_key", updatable = false, nullable = false, length = 128)
  private String paramKey;

  @Column(name = "value", updatable = false, nullable = true, length = 4000)
  private String value;

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
