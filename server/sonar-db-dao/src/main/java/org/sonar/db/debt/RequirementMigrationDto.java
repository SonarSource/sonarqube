/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.debt;

import java.io.Serializable;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Only used in {@link org.sonar.server.startup.CopyRequirementsFromCharacteristicsToRules}
 */
public class RequirementMigrationDto implements Serializable {

  private Integer id;
  private Integer parentId;
  private Integer rootId;
  private Integer ruleId;
  private String functionKey;
  private Double coefficientValue;
  private String coefficientUnit;
  private Double offsetValue;
  private String offsetUnit;
  private Date createdAt;
  private Date updatedAt;
  private boolean enabled;

  public Integer getId() {
    return id;
  }

  public RequirementMigrationDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public Integer getParentId() {
    return parentId;
  }

  public RequirementMigrationDto setParentId(Integer i) {
    this.parentId = i;
    return this;
  }

  public Integer getRootId() {
    return rootId;
  }

  public RequirementMigrationDto setRootId(Integer rootId) {
    this.rootId = rootId;
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  public RequirementMigrationDto setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  public String getFunction() {
    return functionKey;
  }

  public RequirementMigrationDto setFunction(String function) {
    this.functionKey = function;
    return this;
  }

  @CheckForNull
  public Double getCoefficientValue() {
    return coefficientValue;
  }

  public RequirementMigrationDto setCoefficientValue(@Nullable Double coefficientValue) {
    this.coefficientValue = coefficientValue;
    return this;
  }

  @CheckForNull
  public String getCoefficientUnit() {
    return coefficientUnit;
  }

  public RequirementMigrationDto setCoefficientUnit(@Nullable String coefficientUnit) {
    this.coefficientUnit = coefficientUnit;
    return this;
  }

  @CheckForNull
  public Double getOffsetValue() {
    return offsetValue;
  }

  public RequirementMigrationDto setOffsetValue(@Nullable Double offset) {
    this.offsetValue = offset;
    return this;
  }

  @CheckForNull
  public String getOffsetUnit() {
    return offsetUnit;
  }

  public RequirementMigrationDto setOffsetUnit(@Nullable String offsetUnit) {
    this.offsetUnit = offsetUnit;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public RequirementMigrationDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @CheckForNull
  public Date getUpdatedAt() {
    return updatedAt;
  }

  public RequirementMigrationDto setUpdatedAt(@Nullable Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public RequirementMigrationDto setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

}
