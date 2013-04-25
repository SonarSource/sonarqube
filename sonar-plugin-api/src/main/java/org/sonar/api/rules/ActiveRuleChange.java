/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.database.BaseIdentifiable;
import org.sonar.api.profiles.RulesProfile;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * A class to map a RuleChange to the hibernate model
 * 
 * @since 2.9
 */
@Entity
@Table(name = "active_rule_changes")
public class ActiveRuleChange extends BaseIdentifiable {

  @Column(name = "username", updatable = false, nullable = true)
  private String userName;

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

  public ActiveRuleChange(String userName, RulesProfile profile, Rule rule) {
    this.userName = userName;
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

  public int getProfileVersion() {
    return profileVersion;
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

  public String getUserName() {
    return userName;
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
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    ActiveRuleChange rhs = (ActiveRuleChange) obj;
    return new EqualsBuilder()
        .appendSuper(super.equals(obj))
        .append(userName, rhs.userName)
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
        .append(userName)
        .append(rulesProfile)
        .append(rule)
        .append(date)
        .append(enabled)
        .append(newSeverity)
        .toHashCode();
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }

}
