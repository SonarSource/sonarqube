/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

  public static final String INHERITED = "INHERITED";
  public static final String OVERRIDES = "OVERRIDES";

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Integer id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "rule_id", updatable = true, nullable = false)
  private Rule rule;

  @Column(name = "failure_level", updatable = true, nullable = false)
  @Enumerated(EnumType.ORDINAL)
  private RulePriority severity;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "profile_id", updatable = true, nullable = false)
  private RulesProfile rulesProfile;

  @OneToMany(mappedBy = "activeRule", fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REMOVE })
  private List<ActiveRuleParam> activeRuleParams = new ArrayList<ActiveRuleParam>();

  @Column(name = "inheritance", updatable = true, nullable = true)
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
   * @since 2.5
   */
  public void setSeverity(RulePriority severity) {
    this.severity = severity;
  }

  /**
   * @deprecated since 2.5 use {@link #getSeverity()} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public RulePriority getPriority() {
    return severity;
  }

  /**
   * @deprecated since 2.5 use {@link #setSeverity(RulePriority)} instead. See http://jira.codehaus.org/browse/SONAR-1829
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
    return new ToStringBuilder(this).append("id", getId()).append("rule", rule).append("priority", severity)
        .append("params", activeRuleParams).toString();
  }

  @Override
  public Object clone() {
    final ActiveRule clone = new ActiveRule(getRulesProfile(), getRule(), getSeverity());
    clone.setInheritance(getInheritance());
    if (CollectionUtils.isNotEmpty(getActiveRuleParams())) {
      clone.setActiveRuleParams(new ArrayList<ActiveRuleParam>(CollectionUtils.collect(getActiveRuleParams(), new Transformer() {
        public Object transform(Object input) {
          ActiveRuleParam activeRuleParamClone = (ActiveRuleParam) ((ActiveRuleParam) input).clone();
          activeRuleParamClone.setActiveRule(clone);
          return activeRuleParamClone;
        }
      })));
    }
    return clone;
  }

  /**
   * @since 2.6
   */
  public boolean isEnabled() {
    return getRule()!=null && getRule().isEnabled();
  }
}
