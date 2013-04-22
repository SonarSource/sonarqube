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
package org.sonar.wsclient.services;

import java.util.Date;

public class Violation extends Model {

  private Long id = null;
  private String message = null;
  private String severity = null;
  private Integer line = null;
  private String ruleKey = null;
  private String ruleName = null;
  private String resourceKey = null;
  private String resourceName = null;
  private String resourceScope = null;
  private String resourceQualifier = null;
  private Date createdAt = null;
  private boolean switchedOff;
  private Review review = null;

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * @since 2.5
   */
  public String getSeverity() {
    return severity;
  }

  /**
   * @since 2.5
   */
  public void setSeverity(String severity) {
    this.severity = severity;
  }

  /**
   * @deprecated since 2.5 use {@link #getSeverity()} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public String getPriority() {
    return severity;
  }

  /**
   * @deprecated since 2.5 use {@link #setSeverity(String)} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public void setPriority(String priority) {
    this.severity = priority;
  }

  /**
   * @return line number (numeration starts from 1), or <code>null</code> if violation doesn't belong to concrete line
   * @see #hasLine()
   */
  public Integer getLine() {
    return line;
  }

  public void setLine(Integer line) {
    if (line != null && line < 1) {
      /*
       * This shouldn't happen, however line would be normalized to null if web service returns incorrect value (less than 1) in compliance
       * with a contract for getLine method. Normalization added in 2.8 - see http://jira.codehaus.org/browse/SONAR-2386
       */
      this.line = null;
    } else {
      this.line = line;
    }
  }

  /**
   * @return <code>true<code> if violation belongs to concrete line
   * @since 2.8
   */
  public boolean hasLine() {
    return line != null;
  }

  public String getResourceKey() {
    return resourceKey;
  }

  public void setResourceKey(String resourceKey) {
    this.resourceKey = resourceKey;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public Violation setRuleKey(String s) {
    this.ruleKey = s;
    return this;
  }

  public String getRuleName() {
    return ruleName;
  }

  public Violation setRuleName(String ruleName) {
    this.ruleName = ruleName;
    return this;
  }

  public String getResourceName() {
    return resourceName;
  }

  public Violation setResourceName(String resourceName) {
    this.resourceName = resourceName;
    return this;
  }

  public String getResourceScope() {
    return resourceScope;
  }

  public Violation setResourceScope(String resourceScope) {
    this.resourceScope = resourceScope;
    return this;
  }

  public String getResourceQualifier() {
    return resourceQualifier;
  }

  public Violation setResourceQualifier(String resourceQualifier) {
    this.resourceQualifier = resourceQualifier;
    return this;
  }

  /**
   * @since 2.5
   */
  public Date getCreatedAt() {
    return createdAt;
  }

  /**
   * @since 2.5
   */
  public Violation setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * @since 2.5
   */
  public boolean isCreatedAfter(Date date) {
    return createdAt != null && date != null && createdAt.after(date);
  }

  /**
   * @since 2.8
   */
  public Violation setSwitchedOff(Boolean b) {
    this.switchedOff = (b != null && b);
    return this;
  }

  /**
   * @since 2.8
   */
  public boolean isSwitchedOff() {
    return switchedOff;
  }

  /**
   * @since 2.8
   */
  public Review getReview() {
    return review;
  }

  /**
   * @since 2.8
   */
  public Violation setReview(Review review) {
    this.review = review;
    return this;
  }

  /**
   * @since 2.9
   */
  public Long getId() {
    return id;
  }

  /**
   * @since 2.9
   */
  public void setId(Long id) {
    this.id = id;
  }
}
