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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.resources.Resource;

/**
 * A class that represents a violation. A violation happens when a resource does not respect a defined rule.
 */
public class Violation {

  private Resource resource;
  private Rule rule;
  private String message;
  private RulePriority priority;
  private Integer lineId;
  private Double cost;

  /**
   * Creates of a violation from a rule. Will need to define the resource later on
   * @deprecated since 2.3. Use the factory method create()
   */
  @Deprecated
  public Violation(Rule rule) {
    this.rule = rule;
  }

  /**
   * Creates a fully qualified violation
   *
   * @param rule the rule that has been violated
   * @param resource the resource the violation should be attached to
   * @deprecated since 2.3. Use the factory method create()
   */
  @Deprecated
  public Violation(Rule rule, Resource resource) {
    this.resource = resource;
    this.rule = rule;
  }

  public Resource getResource() {
    return resource;
  }

  /**
   * Sets the resource the violation applies to
   *
   * @return the current object
   */
  public Violation setResource(Resource resource) {
    this.resource = resource;
    return this;
  }

  public Rule getRule() {
    return rule;
  }

  /**
   * Sets the rule violated
   *
   * @return the current object
   */
  public Violation setRule(Rule rule) {
    this.rule = rule;
    return this;
  }

  public String getMessage() {
    return message;
  }

  /**
   * Sets the violation message
   *
   * @return the current object
   */
  public Violation setMessage(String message) {
    this.message = message;
    return this;
  }

  public Integer getLineId() {
    return lineId;
  }

  /**
   * Sets the violation line
   *
   * @return the current object
   */
  public Violation setLineId(Integer lineId) {
    this.lineId = lineId;
    return this;
  }

  public RulePriority getPriority() {
    return priority;
  }

  /**
   * Sets the violation priority
   *
   * @return the current object
   * @deprecated since 2.3. The priority is set by the quality profile.
   */
  @Deprecated
  public Violation setPriority(RulePriority priority) {
    this.priority = priority;
    return this;
  }

  /**
   * @see <code>setCost()</code>
   */
  public Double getCost() {
    return cost;
  }

  /**
   * The cost to fix a violation can't be precisely computed without this information.
   * Let's take the following example : a rule forbids to have methods whose complexity is greater than 10. Without this field "cost",
   * the same violation is created with a method whose complexity is 15 and a method whose complexity is 100.
   * If the cost to fix one point of complexity is 0.05h, then 15mn is necessary to fix the method whose complexity is 15,
   * and 3h5mn is required to fix the method whose complexity is 100.
   */
  public Violation setCost(Double d) {
    this.cost = d;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Violation)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    Violation other = (Violation) obj;
    return new EqualsBuilder()
        .append(rule, other.getRule())
        .append(resource, other.getResource())
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(getRule())
        .append(getResource())
        .toHashCode();
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  public static Violation create(ActiveRule activeRule, Resource resource) {
    return new Violation(activeRule.getRule()).setResource(resource);
  }

  public static Violation create(Rule rule, Resource resource) {
    return new Violation(rule).setResource(resource);
  }
}
