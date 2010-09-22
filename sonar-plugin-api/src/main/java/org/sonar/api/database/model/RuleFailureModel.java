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
package org.sonar.api.database.model;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.database.BaseIdentifiable;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;

import javax.persistence.*;

@Entity
@Table(name = "rule_failures")
public class RuleFailureModel extends BaseIdentifiable {

  public static final int MESSAGE_COLUMN_SIZE = 4000;

  @Column(name = "snapshot_id")
  protected Integer snapshotId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "rule_id")
  private Rule rule;

  @Column(name = "failure_level", updatable = false, nullable = false)
  @Enumerated(EnumType.ORDINAL)
  private RulePriority priority;

  @Column(name = "message", updatable = false, nullable = true, length = MESSAGE_COLUMN_SIZE)
  private String message;

  @Column(name = "line", updatable = true, nullable = true)
  private Integer line;

  @Column(name = "cost", updatable = true, nullable = true)
  private Double cost;

  public RuleFailureModel() {
  }

  public RuleFailureModel(Rule rule, RulePriority priority) {
    this.rule = rule;
    this.priority = priority;
  }


  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = StringUtils.abbreviate(StringUtils.trim(message), MESSAGE_COLUMN_SIZE);
  }

  public RulePriority getLevel() {
    return priority;
  }

  public void setLevel(RulePriority priority) {
    this.priority = priority;
  }

  public Rule getRule() {
    return rule;
  }

  public void setRule(Rule rule) {
    this.rule = rule;
  }

  public Integer getLine() {
    return line;
  }

  public RulePriority getPriority() {
    return priority;
  }

  public Integer getSnapshotId() {
    return snapshotId;
  }

  public void setSnapshotId(Integer snapshotId) {
    this.snapshotId = snapshotId;
  }

  public void setPriority(RulePriority priority) {
    this.priority = priority;
  }

  public void setLine(Integer line) {
    this.line = line;
  }

  public Double getCost() {
    return cost;
  }

  public RuleFailureModel setCost(Double d) {
    this.cost = d;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RuleFailureModel)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    RuleFailureModel other = (RuleFailureModel) obj;
    return new EqualsBuilder()
        .append(getId(), other.getId()).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).
        append(getId()).toHashCode();
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }
}
