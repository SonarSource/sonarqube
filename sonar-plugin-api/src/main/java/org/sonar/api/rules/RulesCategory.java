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
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.database.BaseIdentifiable;

/**
 * @deprecated since 2.5 See http://jira.codehaus.org/browse/SONAR-2007
 */
@Deprecated
public class RulesCategory extends BaseIdentifiable {

  private String name;
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
   * @param name        the category name
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
   *
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
}
