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
  private String sortDirection;
  private Long orderIndex;
  private Boolean variation;

  public Long getId() {
    return id;
  }

  public FilterColumnDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getFilterId() {
    return filterId;
  }

  public FilterColumnDto setFilterId(Long filterId) {
    this.filterId = filterId;
    return this;
  }

  public String getFamily() {
    return family;
  }

  public FilterColumnDto setFamily(String family) {
    this.family = family;
    return this;
  }

  public String getKey() {
    return key;
  }

  public FilterColumnDto setKey(String key) {
    this.key = key;
    return this;
  }

  public String getSortDirection() {
    return sortDirection;
  }

  public FilterColumnDto setSortDirection(String sortDirection) {
    this.sortDirection = sortDirection;
    return this;
  }

  public Long getOrderIndex() {
    return orderIndex;
  }

  public FilterColumnDto setOrderIndex(Long orderIndex) {
    this.orderIndex = orderIndex;
    return this;
  }

  public Boolean getVariation() {
    return variation;
  }

  public FilterColumnDto setVariation(Boolean variation) {
    this.variation = variation;
    return this;
  }
}
