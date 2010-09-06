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
package org.sonar.api.measures;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;

/**
 * @since 1.10
 */
public class RuleMeasure extends Measure {

  private Rule rule;
  private RulePriority rulePriority;
  private Integer ruleCategory;

  /**
   * This constructor is for internal use only. Please use static methods createForXXX().
   */
  public RuleMeasure(Metric metric, Rule rule, RulePriority rulePriority, Integer ruleCategory) {
    super(metric);
    this.rule = rule;
    this.rulePriority = rulePriority;
    this.ruleCategory = ruleCategory;
  }

  public Rule getRule() {
    return rule;
  }

  public void setRule(Rule rule) {
    this.rule = rule;
  }

  public RulePriority getRulePriority() {
    return rulePriority;
  }

  public void setRulePriority(RulePriority rulePriority) {
    this.rulePriority = rulePriority;
  }

  public Integer getRuleCategory() {
    return ruleCategory;
  }

  public void setRuleCategory(Integer ruleCategory) {
    this.ruleCategory = ruleCategory;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj.getClass().equals(RuleMeasure.class))) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    RuleMeasure other = (RuleMeasure) obj;
    return new EqualsBuilder()
        .append(getMetric(), other.getMetric())
        .append(rule, other.rule)
        .append(rulePriority, other.rulePriority)
        .append(ruleCategory, other.ruleCategory)
        .isEquals();
  }

  @Override
  public RuleMeasure setValue(Double v) {
    return (RuleMeasure) super.setValue(v);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).
        append(getMetric()).
        append(rule).
        append(rulePriority).
        append(ruleCategory).
        toHashCode();

  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).
        append("id", getId()).
        append("metric", metric).
        append("value", value).
        append("data", data).
        append("description", description).
        append("alertStatus", alertStatus).
        append("alertText", alertText).
        append("tendency", tendency).
        append("rule", rule).
        append("category", ruleCategory).
        append("priority", rulePriority).
        toString();
  }

  public static RuleMeasure createForRule(Metric metric, Rule rule, Double value) {
    return (RuleMeasure) new RuleMeasure(metric, rule, null, null).setValue(value);
  }

  public static RuleMeasure createForPriority(Metric metric, RulePriority priority, Double value) {
    return (RuleMeasure) new RuleMeasure(metric, null, priority, null).setValue(value);
  }

  public static RuleMeasure createForCategory(Metric metric, Integer category, Double value) {
    return (RuleMeasure) new RuleMeasure(metric, null, null, category).setValue(value);
  }
}
