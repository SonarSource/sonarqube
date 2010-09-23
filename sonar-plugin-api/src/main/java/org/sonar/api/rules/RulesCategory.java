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
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.sonar.api.database.BaseIdentifiable;
import org.sonar.check.IsoCategory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * A class to hold rules category
 */
@Immutable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Entity
@Table(name = "rules_categories")
public class RulesCategory extends BaseIdentifiable {

  @Column(name = "name", updatable = false, nullable = false)
  private String name;

  @Column(name = "description", updatable = false, nullable = true)
  private String description;

  /**
   * Creates a RuleCategory based on the category name
   *
   * @param name the category name
   */
  public RulesCategory(String name) {
    this.name = name;
  }

  /**
   * Creates a category based on the category name and description
   *
   * @param name the category name
   * @param description the category description
   */
  public RulesCategory(String name, String description) {
    this.name = name;
    this.description = description;
  }

  /**
   * Creates an empty category
   */
  public RulesCategory() {
  }

  /**
   * @return the category name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the category name
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the category description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the cay description
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RulesCategory)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    RulesCategory other = (RulesCategory) obj;
    return new EqualsBuilder()
        .append(name, other.getName()).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(name)
        .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", getId())
        .append("name", name)
        .append("desc", description)
        .toString();
  }

  public IsoCategory toIsoCategory() {
    if (name.equals(Iso9126RulesCategories.EFFICIENCY.getName())) {
      return IsoCategory.Efficiency;
    }
    if (name.equals(Iso9126RulesCategories.MAINTAINABILITY.getName())) {
      return IsoCategory.Maintainability;
    }
    if (name.equals(Iso9126RulesCategories.PORTABILITY.getName())) {
      return IsoCategory.Portability;
    }
    if (name.equals(Iso9126RulesCategories.RELIABILITY.getName())) {
      return IsoCategory.Reliability;
    }
    if (name.equals(Iso9126RulesCategories.USABILITY.getName())) {
      return IsoCategory.Usability;
    }
    return null;
  }

  public static RulesCategory fromIsoCategory(IsoCategory iso) {
    if (iso==IsoCategory.Efficiency) {
      return Iso9126RulesCategories.EFFICIENCY;
    }
    if (iso==IsoCategory.Maintainability) {
      return Iso9126RulesCategories.MAINTAINABILITY;
    }
    if (iso==IsoCategory.Portability) {
      return Iso9126RulesCategories.PORTABILITY;
    }
    if (iso==IsoCategory.Reliability) {
      return Iso9126RulesCategories.RELIABILITY;
    }
    if (iso==IsoCategory.Usability) {
      return Iso9126RulesCategories.USABILITY;
    }
    return null;
  }
}
