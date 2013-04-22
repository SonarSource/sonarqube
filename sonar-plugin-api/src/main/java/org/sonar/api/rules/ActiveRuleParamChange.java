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

import org.sonar.api.database.BaseIdentifiable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.*;

/**
 * @since 2.9
 */
@Entity
@Table(name = "active_rule_param_changes")
public class ActiveRuleParamChange extends BaseIdentifiable {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "active_rule_change_id")
  private ActiveRuleChange activeRuleChange;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "rules_parameter_id")
  private RuleParam ruleParam;

  @Column(name = "old_value", updatable = false, nullable = true, length = 4000)
  private String oldValue;

  @Column(name = "new_value", updatable = false, nullable = true, length = 4000)
  private String newValue;

  ActiveRuleParamChange(ActiveRuleChange activeRuleChange, RuleParam ruleParam, String oldValue, String newValue) {
    this.activeRuleChange = activeRuleChange;
    this.ruleParam = ruleParam;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  public ActiveRuleChange getActiveRuleChange() {
    return activeRuleChange;
  }

  public RuleParam getRuleParam() {
    return ruleParam;
  }

  public String getOldValue() {
    return oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public String getKey() {
    return ruleParam.getKey();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ActiveRuleParamChange)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    ActiveRuleParamChange other = (ActiveRuleParamChange) obj;
    return new EqualsBuilder()
        .append(getId(), other.getId()).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 57)
        .append(getId())
        .toHashCode();
  }

}
