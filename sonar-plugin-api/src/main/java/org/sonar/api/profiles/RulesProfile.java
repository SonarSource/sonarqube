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
package org.sonar.api.profiles;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rules_profiles")
public class RulesProfile implements Cloneable {

  /**
   * The profile key for the embedded profile Sonar Way
   */
  public static final String SONAR_WAY_NAME = "Sonar way";

  /**
   * The profile key for the embedded profile Sonar Way with Findbugs
   */
  public static final String SONAR_WAY_FINDBUGS_NAME = "Sonar way with Findbugs";

  /**
   * The profile key for the embedded profile Sun checks
   */
  public static final String SUN_CONVENTIONS_NAME = "Sun checks";

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Integer id;

  @Column(name = "name", updatable = true, nullable = false)
  private String name;

  @Column(name = "default_profile", updatable = true, nullable = false)
  private Boolean defaultProfile = Boolean.FALSE;

  @Column(name = "provided", updatable = true, nullable = false)
  private Boolean provided = Boolean.FALSE;

  @Column(name = "language", updatable = true, nullable = false)
  private String language;

  @OneToMany(mappedBy = "rulesProfile", fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REMOVE})
  private List<ActiveRule> activeRules = new ArrayList<ActiveRule>();

  @OneToMany(mappedBy = "rulesProfile", fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REMOVE})
  private List<Alert> alerts = new ArrayList<Alert>();

  @OneToMany(mappedBy = "rulesProfile", fetch = FetchType.LAZY)
  private List<ResourceModel> projects = new ArrayList<ResourceModel>();

  /**
   * @deprecated use the factory method create()
   */
  @Deprecated
  public RulesProfile() {
  }

  /**
   * <p>Creates a profile of rules with empty active rules, empty alerts and empty project lists.</p>
   *
   * @param name     the name to be used to access the profile, will be used as a key and display name
   * @param language the language to which this profile applies
   * @deprecated use the factory method create()
   */
  @Deprecated
  public RulesProfile(String name, String language) {
    this.name = name;
    this.language = language;
    this.activeRules = new ArrayList<ActiveRule>();
    this.alerts = new ArrayList<Alert>();
    this.projects = new ArrayList<ResourceModel>();
  }

  /**
   * <p>Creates a profile of rules with empty active rules, empty alerts and empty project lists.</p>
   *
   * @param name           the name to be used to access the profile, will be used as a key and display name
   * @param language       the language to which this profile applies
   * @param defaultProfile whether this is the default profile for the language
   * @param provided       whether the profile is embarked in core Sonar
   */
  @Deprecated
  public RulesProfile(String name, String language, boolean defaultProfile, boolean provided) {
    this(name, language);
    this.defaultProfile = defaultProfile;
    this.provided = provided;
  }

  public Integer getId() {
    return id;
  }

  /**
   * @return the name of the profile
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the profile
   */
  public RulesProfile setName(String s) {
    this.name = s;
    return this;
  }

  /**
   * @return the list of active rules
   */
  public List<ActiveRule> getActiveRules() {
    return activeRules;
  }

  /**
   * Sets the list of active rules
   */
  public void setActiveRules(List<ActiveRule> activeRules) {
    this.activeRules = activeRules;
  }

  /**
   * @return whether this is the default profile for the language
   */
  public Boolean getDefaultProfile() {
    return defaultProfile;
  }

  /**
   * Sets whether this is the default profile for the language
   */
  public void setDefaultProfile(Boolean b) {
    this.defaultProfile = b;
  }

  /**
   * @return whether the profile ships with Sonar core
   */
  public Boolean getProvided() {
    return provided;
  }

  /**
   * Sets wether the profile ships with Sonar core
   */
  public void setProvided(Boolean provided) {
    this.provided = provided;
  }

  /**
   * @return the language of the profile
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Sets the language for the profile
   */
  public RulesProfile setLanguage(String s) {
    this.language = s;
    return this;
  }

  /**
   * @return the list of alerts defined in the profile
   */
  public List<Alert> getAlerts() {
    return alerts;
  }

  /**
   * Sets the list of alerts for the profile
   */
  public void setAlerts(List<Alert> alerts) {
    this.alerts = alerts;
  }

  /**
   * @return the list of projects attached to the profile
   */
  public List<ResourceModel> getProjects() {
    return projects;
  }

  /**
   * Sets the list of projects attached to the profile
   */
  public void setProjects(List<ResourceModel> projects) {
    this.projects = projects;
  }

  /**
   * @return the list of active rules for a given priority
   */
  public List<ActiveRule> getActiveRules(RulePriority priority) {
    List<ActiveRule> result = new ArrayList<ActiveRule>();
    for (ActiveRule activeRule : getActiveRules()) {
      if (activeRule.getPriority().equals(priority)) {
        result.add(activeRule);
      }
    }
    return result;
  }

  /**
   * @deprecated use getActiveRulesByRepository()
   */
  @Deprecated
  public List<ActiveRule> getActiveRulesByPlugin(String repositoryKey) {
    return getActiveRulesByRepository(repositoryKey);
  }

  public List<ActiveRule> getActiveRulesByRepository(String repositoryKey) {
    List<ActiveRule> result = new ArrayList<ActiveRule>();
    for (ActiveRule activeRule : getActiveRules()) {
      if (repositoryKey.equals(activeRule.getPluginName())) {
        result.add(activeRule);
      }
    }
    return result;
  }

  /**
   * @return an active rule from a plugin key and a rule key if the rule is activated, null otherwise
   */
  public ActiveRule getActiveRule(String repositoryKey, String ruleKey) {
    for (ActiveRule activeRule : getActiveRules()) {
      if (StringUtils.equals(activeRule.getRepositoryKey(), repositoryKey) && StringUtils.equals(activeRule.getRuleKey(), ruleKey)) {
        return activeRule;
      }
    }
    return null;
  }

  public ActiveRule getActiveRuleByConfigKey(String repositoryKey, String configKey) {
    for (ActiveRule activeRule : getActiveRules()) {
      if (StringUtils.equals(activeRule.getRepositoryKey(), repositoryKey) && StringUtils.equals(activeRule.getConfigKey(), configKey)) {
        return activeRule;
      }
    }
    return null;
  }

  public ActiveRule getActiveRule(Rule rule) {
    return getActiveRule(rule.getRepositoryKey(), rule.getKey());
  }

  /**
   *
   * @param rule
   * @param optionalPriority if null, then the default rule priority is used
   * @return
   */
  public ActiveRule activateRule(Rule rule, RulePriority optionalPriority) {
    ActiveRule activeRule = new ActiveRule();
    activeRule.setRule(rule);
    activeRule.setRulesProfile(this);
    activeRule.setPriority(optionalPriority==null ? rule.getPriority() : optionalPriority);
    activeRules.add(activeRule);
    return activeRule;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RulesProfile)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    RulesProfile other = (RulesProfile) obj;
    return new EqualsBuilder().append(language, other.getLanguage()).append(name, other.getName()).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(language).append(name).toHashCode();
  }

  @Override
  public Object clone() {
    RulesProfile clone = new RulesProfile(getName(), getLanguage(), getDefaultProfile(), getProvided());
    if (CollectionUtils.isNotEmpty(getActiveRules())) {
      clone.setActiveRules(new ArrayList<ActiveRule>(CollectionUtils.collect(getActiveRules(), new Transformer() {
        public Object transform(Object input) {
          return ((ActiveRule) input).clone();
        }
      })));
    }
    if (CollectionUtils.isNotEmpty(getAlerts())) {
      clone.setAlerts(new ArrayList<Alert>(CollectionUtils.collect(getAlerts(), new Transformer() {
        public Object transform(Object input) {
          return ((Alert) input).clone();
        }
      })));
    }
    if (CollectionUtils.isNotEmpty(getProjects())) {
      clone.setProjects(new ArrayList<ResourceModel>(CollectionUtils.collect(getProjects(), new Transformer() {
        public Object transform(Object input) {
          return ((ResourceModel) input).clone();
        }
      })));
    }
    return clone;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("[name=").append(name).append(",language=").append(language).append("]").toString();
  }

  public static RulesProfile create(String name, String language) {
    return new RulesProfile().setName(name).setLanguage(language);
  }

  public static RulesProfile create() {
    return new RulesProfile();
  }
}
