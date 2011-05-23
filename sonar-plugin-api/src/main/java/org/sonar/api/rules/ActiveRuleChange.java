/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.database.BaseIdentifiable;
import org.sonar.api.profiles.RulesProfile;

/**
 * A class to map a RuleChange to the hibernate model
 */
@Entity
@Table(name = "active_rule_changes")
public class ActiveRuleChange extends BaseIdentifiable {

  @Column(name = "user_login", updatable = false, nullable = false)
  private String modifierLogin;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "profile_id", updatable = false, nullable = false)
  private RulesProfile rulesProfile;

  @Column(name = "profile_version", updatable = false, nullable = false)
  private int profileVersion;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "rule_id", updatable = false, nullable = false)
  private Rule rule;
  
  @Column(name = "change_date", updatable = false, nullable = false)
  private Date date;
  
  /**
   * true means rule was enabled
   * false means rule was disabled
   * null means rule stay enabled (another param was changed)
   */
  @Column(name = "enabled")
  private Boolean enabled;
  
  @Column(name = "old_severity", updatable = false, nullable = true)
  @Enumerated(EnumType.ORDINAL)
  private RulePriority oldSeverity;

  @Column(name = "new_severity", updatable = false, nullable = true)
  @Enumerated(EnumType.ORDINAL)
  private RulePriority newSeverity;
  
  @OneToMany(mappedBy = "activeRuleChange", fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REMOVE })
  private List<ActiveRuleParamChange> activeRuleParamChanges = new ArrayList<ActiveRuleParamChange>();

  
  public ActiveRuleChange(String modifierLogin, RulesProfile profile, Rule rule) {
    this.modifierLogin = modifierLogin;
    this.rulesProfile = profile;
    this.profileVersion = profile.getVersion();
    this.rule = rule;
    this.date = Calendar.getInstance().getTime();
  }

  public Rule getRule() {
    return rule;
  }

  public RulePriority getOldSeverity() {
    return oldSeverity;
  }
  
  public void setOldSeverity(RulePriority oldSeverity) {
    this.oldSeverity = oldSeverity;
  }

  public RulePriority getNewSeverity() {
    return newSeverity;
  }
  
  public void setNewSeverity(RulePriority newSeverity) {
    this.newSeverity = newSeverity;
  }

  public RulesProfile getRulesProfile() {
    return rulesProfile;
  }

  public String getRepositoryKey() {
    return rule.getRepositoryKey();
  }

  /**
   * @return the config key the changed rule belongs to
   */
  public String getConfigKey() {
    return rule.getConfigKey();
  }

  /**
   * @return the key of the changed rule
   */
  public String getRuleKey() {
    return rule.getKey();
  }
  
  public Boolean isEnabled() {
    return enabled;
  }
  
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public List<ActiveRuleParamChange> getActiveRuleParamChanges() {
    return activeRuleParamChanges;
  }
  
  public String getModifierLogin() {
    return modifierLogin;
  }

  public ActiveRuleChange setParameterChange(String key, String oldValue, String newValue) {
    RuleParam ruleParameter = rule.getParam(key);
    if (ruleParameter != null) {
      activeRuleParamChanges.add(new ActiveRuleParamChange(this, ruleParameter, oldValue, newValue));
    }
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) { return false; }
    if (obj == this) { return true; }
    if (obj.getClass() != getClass()) {
      return false;
    }
    ActiveRuleChange rhs = (ActiveRuleChange) obj;
    return new EqualsBuilder()
        .appendSuper(super.equals(obj))
        .append(modifierLogin, rhs.modifierLogin)
        .append(rulesProfile, rhs.rulesProfile)
        .append(rule, rhs.rule)
        .append(date, rhs.date)
        .append(enabled, rhs.enabled)
        .append(newSeverity, rhs.newSeverity)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(41, 33)
        .append(modifierLogin)
        .append(rulesProfile)
        .append(rule)
        .append(date)
        .append(enabled)
        .append(newSeverity)
        .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("id", getId()).append("profile", rulesProfile).append("rule", rule)
        .append("modifier", modifierLogin).append("changed at", date).append("enabled", enabled).append("new severity", newSeverity)
        .toString();
  }

}
