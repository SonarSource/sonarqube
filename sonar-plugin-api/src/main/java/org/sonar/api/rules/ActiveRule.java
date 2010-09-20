/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.rules;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.profiles.RulesProfile;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;

/**
 * A class to map an ActiveRule to the hibernate model
 */
@Entity
@Table(name = "active_rules")
public class ActiveRule implements Cloneable {

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Integer id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "rule_id", updatable = true, nullable = false)
  private Rule rule;

  @Column(name = "failure_level", updatable = true, nullable = false)
  @Enumerated(EnumType.ORDINAL)
  private RulePriority priority;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "profile_id", updatable = true, nullable = false)
  private RulesProfile rulesProfile;

  @OneToMany(mappedBy = "activeRule", fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REMOVE})
  private List<ActiveRuleParam> activeRuleParams = new ArrayList<ActiveRuleParam>();

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
  public ActiveRule(RulesProfile profile, Rule rule, RulePriority priority) {
    this.rule = rule;
    if (priority == null && rule != null) {
      this.priority = rule.getPriority();
    } else {
      this.priority = priority;
    }

    this.rulesProfile = profile;
  }

  public Integer getId() {
    return id;
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

  public RulePriority getPriority() {
    return priority;
  }

  public void setPriority(RulePriority priority) {
    this.priority = priority;
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
   * @deprecated use getRepositoryKey()
   */
  @Deprecated
  public String getPluginName() {
    return rule.getPluginName();
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
    if (rulesProfile != null ? !rulesProfile.equals(that.rulesProfile) : that.rulesProfile != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = rule.hashCode();
    result = 31 * result + (rulesProfile != null ? rulesProfile.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("id", getId()).append("rule", rule).append("priority", priority).append("params", activeRuleParams).toString();
  }

  @Override
  public Object clone() {
    ActiveRule clone = new ActiveRule(getRulesProfile(), getRule(), getPriority());
    if (CollectionUtils.isNotEmpty(getActiveRuleParams())) {
      clone.setActiveRuleParams(new ArrayList<ActiveRuleParam>(CollectionUtils.collect(getActiveRuleParams(), new Transformer() {
        public Object transform(Object input) {
          return ((ActiveRuleParam) input).clone();
        }
      })));
    }
    return clone;
  }

}
