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
package org.sonar.core.filter;

/**
 * @since 3.1
 */
public final class FilterColumnDto {
  private Long id;
  private Long filterId;
  private String family;
  private String key;
  private Long orderIndex;
  private String sortDirection;
  private Boolean variation;

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @return the family
   */
  public String getFamily() {
    return family;
  }

  /**
   * @return the filter id
   */
  public Long getFilterId() {
    return filterId;
  }

  /**
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * @return the order index
   */
  public Long getOrderIndex() {
    return orderIndex;
  }

  /**
   * @return the sort direction
   */
  public String getSortDirection() {
    return sortDirection;
  }

  /**
   * @return the variation
   */
  public Boolean getVariation() {
    return variation;
  }

  /**
   * @param family the family to set
   */
  public FilterColumnDto setFamily(String family) {
    this.family = family;
    return this;
  }

  /**
   * @param family the id to set
   */
  public FilterColumnDto setId(Long id) {
    this.id = id;
    return this;
  }

  /**
   * @param filterId the filterId to set
   */
  public FilterColumnDto setFilterId(Long filterId) {
    this.filterId = filterId;
    return this;
  }

  /**
   * @param key the key to set
   */
  public FilterColumnDto setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * @param orderIndex the orderIndex to set
   */
  public FilterColumnDto setOrderIndex(Long orderIndex) {
    this.orderIndex = orderIndex;
    return this;
  }

  /**
   * @param sortDirection the sortDirection to set
   */
  public FilterColumnDto setSortDirection(String sortDirection) {
    this.sortDirection = sortDirection;
    return this;
  }

  /**
   * @param variation the variation to set
   */
  public FilterColumnDto setVariation(Boolean variation) {
    this.variation = variation;
    return this;
  }
}
