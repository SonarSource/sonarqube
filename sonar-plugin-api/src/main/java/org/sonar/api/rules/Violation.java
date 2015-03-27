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

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.log.Loggers;

import java.util.Date;

/**
 * A class that represents a violation. A violation happens when a resource does not respect a defined rule.
 * @deprecated in 3.6. Replaced by {@link org.sonar.api.issue.Issue}.
 */
@Deprecated
public class Violation {

  private Resource resource;
  private Rule rule;
  private String message;
  private RulePriority severity;
  private Integer lineId;
  private Double cost;
  private Date createdAt;
  private boolean switchedOff = false;
  private String checksum;
  private boolean isNew = false;
  private boolean isManual = false;
  private Integer permanentId;
  private Integer personId;

  /**
   * Creates of a violation from a rule. Will need to define the resource later on
   *
   * @deprecated since 2.3. Use the factory method {@link #create(ActiveRule, Resource)}
   */
  @Deprecated
  public Violation(Rule rule) {
    this.rule = rule;
  }

  /**
   * Creates a fully qualified violation
   *
   * @param rule     the rule that has been violated
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

  /**
   * @return line number (numeration starts from 1), or <code>null</code> if violation doesn't belong to concrete line
   * @see #hasLineId()
   */
  public Integer getLineId() {
    return lineId;
  }

  /**
   * Sets the violation line.
   *
   * @param lineId line number (numeration starts from 1), or <code>null</code> if violation doesn't belong to concrete line
   * @return the current object
   */
  public Violation setLineId(Integer lineId) {
    if (lineId != null && lineId < 1) {
      // TODO this normalization was added in 2.8, throw exception in future versions - see http://jira.codehaus.org/browse/SONAR-2386
      Loggers.get(getClass()).warn("line must not be less than 1 - in future versions this will cause IllegalArgumentException");
      this.lineId = null;
    } else {
      this.lineId = lineId;
    }
    return this;
  }

  /**
   * @return <code>true<code> if violation belongs to concrete line
   * @since 2.8
   */
  public boolean hasLineId() {
    return lineId != null;
  }

  /**
   * @since 2.5
   */
  public RulePriority getSeverity() {
    return severity;
  }

  /**
   * For internal use only.
   *
   * @since 2.5
   */
  public Violation setSeverity(RulePriority severity) {
    this.severity = severity;
    return this;
  }

  /**
   * @deprecated since 2.5 use {@link #getSeverity()} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public RulePriority getPriority() {
    return severity;
  }

  /**
   * For internal use only
   *
   * @deprecated since 2.5 use {@link #setSeverity(RulePriority)} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public Violation setPriority(RulePriority priority) {
    this.severity = priority;
    return this;
  }

  /**
   * @see #setCost(Double)
   * @since 2.4
   */
  public Double getCost() {
    return cost;
  }

  /**
   * The cost to fix a violation can't be precisely computed without this information. Let's take the following example : a rule forbids to
   * have methods whose complexity is greater than 10. Without this field "cost", the same violation is created with a method whose
   * complexity is 15 and a method whose complexity is 100. If the cost to fix one point of complexity is 0.05h, then 15mn is necessary to
   * fix the method whose complexity is 15, and 3h5mn is required to fix the method whose complexity is 100.
   *
   * @since 2.4
   */
  public Violation setCost(Double d) {
    if (d == null || d >= 0) {
      this.cost = d;
      return this;
    } else {
      throw new IllegalArgumentException("Cost to fix violation can't be negative or NaN");
    }
  }

  /**
   * @since 2.5
   */
  public Date getCreatedAt() {
    return createdAt;
  }

  /**
   * For internal use only
   *
   * @since 2.5
   */
  public Violation setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Switches off the current violation. This is a kind of "mute", which means the violation exists but won't be counted as an active
   * violation (and thus, won't be counted in the total number of violations). It's usually used for false-positives.
   * <p/>
   * The extensions which call this method must be executed
   *
   * @param b if true, the violation is considered OFF
   * @since 2.8
   */
  public Violation setSwitchedOff(boolean b) {
    this.switchedOff = b;
    return this;
  }

  /**
   * Tells whether this violation is ON or OFF.
   *
   * @since 2.8
   */
  public boolean isSwitchedOff() {
    return switchedOff;
  }

  /**
   * Checksum is available in decorators executed after the barrier {@link org.sonar.api.batch.DecoratorBarriers#END_OF_VIOLATION_TRACKING}
   */
  public String getChecksum() {
    return checksum;
  }

  /**
   * For internal use only. Checksum is automatically set by Sonar. Plugins must not call this method.
   */
  public Violation setChecksum(String s) {
    this.checksum = s;
    return this;
  }

  /**
   * A violation is considered as "new" if it has been created after the reference analysis
   * (the "previous" analysis).
   * This method must be used only by post-jobs and decorators depending on the barrier
   * {@link org.sonar.api.batch.DecoratorBarriers#END_OF_VIOLATION_TRACKING}
   *
   * @since 2.9
   */
  public boolean isNew() {
    return isNew;
  }

  /**
   * For internal use only. MUST NOT BE SET FROM PLUGINS.
   *
   * @since 2.9
   */
  public Violation setNew(boolean b) {
    isNew = b;
    return this;
  }

  /**
   * @since 2.13
   */
  public boolean isManual() {
    return isManual;
  }

  /**
   * For internal use only. MUST NOT BE SET FROM PLUGINS.
   *
   * @since 2.13
   */
  public Violation setManual(boolean b) {
    isManual = b;
    return this;
  }

  /**
   * For internal use only. MUST NOT BE SET FROM PLUGINS.
   *
   * @since 2.13
   */
  public Integer getPermanentId() {
    return permanentId;
  }

  /**
   * For internal use only. MUST NOT BE SET FROM PLUGINS.
   *
   * @since 2.13
   */
  public Violation setPermanentId(Integer i) {
    this.permanentId = i;
    return this;
  }

  /**
   * @since 2.13
   */
  public Integer getPersonId() {
    return personId;
  }

  /**
   * For internal use only.
   * 
   * @since 2.13
   */
  public Violation setPersonId(Integer i) {
    this.personId = i;
    return this;
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
