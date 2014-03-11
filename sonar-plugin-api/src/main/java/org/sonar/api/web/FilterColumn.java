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
package org.sonar.api.web;

import com.google.common.collect.ImmutableSortedSet;

import java.util.Set;

import com.google.common.base.Preconditions;

/**
 * Definition of a {@link Filter} column.
 *
 * @since 3.1
 */
public final class FilterColumn {
  public static final String ASC = "ASC";
  public static final String DESC = "DESC";
  public static final Set<String> DIRECTIONS = ImmutableSortedSet.of(ASC, DESC);

  private final String family;
  private final String key;
  private final String sortDirection;
  private final boolean variation;

  private FilterColumn(String family, String key, String sortDirection, boolean variation) {
    Preconditions.checkArgument(DIRECTIONS.contains(sortDirection), "Valid directions are %s, not '%s'", DIRECTIONS, sortDirection);

    this.family = family;
    this.key = key;
    this.sortDirection = sortDirection;
    this.variation = variation;
  }

  /**
   * Creates a new {@link FilterColumn}.
   *
   * <p>Valid values for the {@code sortDirection} are {@code #ASC}, {@code #DESC}</p>
   *
   * <p>When the @{see Filter} is persisted, a validation is made on the {@code family} and the {@code key}.
   * They should point to a valid column description.</p>
   *
   * @throws IllegalArgumentException if {@code sortDirection} is not valid
   */
  public static FilterColumn create(String family, String key, String sortDirection, boolean variation) {
    return new FilterColumn(family, key, sortDirection, variation);
  }

  /**
   * Get the the column's family.
   * 
   * @return the family
   */
  public String getFamily() {
    return family;
  }

  /**
   * Get the the column's key.
   * 
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * Get the the column's sort direction.
   * 
   * @return the sort direction
   */
  public String getSortDirection() {
    return sortDirection;
  }

  /**
   * A column can be based on the variation of a value rather than on the value itself.
   * 
   * @return <code>true</code> when the variation is used rather than the value
   */
  public boolean isVariation() {
    return variation;
  }
}
