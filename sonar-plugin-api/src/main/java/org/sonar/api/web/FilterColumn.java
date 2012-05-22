/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.api.web;

/**
 * Definition of a {@see Filter} column.
 *
 * @since 3.1
 */
public class FilterColumn {
  private String family;
  private String key;
  private String sortDirection;
  private Long orderIndex;
  private boolean variation;

  private FilterColumn() {
    // The factory method should be used
  }

  /**
   * Creates a new {@link FilterColumn}.
   */
  public static FilterColumn create() {
    return new FilterColumn();
  }

  public String getFamily() {
    return family;
  }

  public FilterColumn setFamily(String family) {
    this.family = family;
    return this;
  }

  public String getKey() {
    return key;
  }

  public FilterColumn setKey(String key) {
    this.key = key;
    return this;
  }

  public String getSortDirection() {
    return sortDirection;
  }

  public FilterColumn setSortDirection(String sortDirection) {
    this.sortDirection = sortDirection;
    return this;
  }

  public Long getOrderIndex() {
    return orderIndex;
  }

  public FilterColumn setOrderIndex(Long orderIndex) {
    this.orderIndex = orderIndex;
    return this;
  }

  public boolean isVariation() {
    return variation;
  }

  public FilterColumn setVariation(boolean variation) {
    this.variation = variation;
    return this;
  }
}
