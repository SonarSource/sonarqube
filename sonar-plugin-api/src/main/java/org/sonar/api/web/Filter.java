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
 * Definition of a filter.
 * <p/>
 * Its name can be retrieved using the i18n mechanism, using the keys "dashboard.&lt;id&gt;.name".
 *
 * @since 3.1
 */
public class Filter {
  // Criteria
  // Columns
  //
  private boolean shared;
  private boolean favouritesOnly;
  private String defaultPeriod;

  private Filter() {
    // The factory method should be used
  }

  public boolean isShared() {
    return shared;
  }

  public void setShared(boolean shared) {
    this.shared = shared;
  }

  public boolean isFavouritesOnly() {
    return favouritesOnly;
  }

  public void setFavouritesOnly(boolean favouritesOnly) {
    this.favouritesOnly = favouritesOnly;
  }

  public String getDefaultPeriod() {
    return defaultPeriod;
  }

  public void setDefaultPeriod(String defaultPeriod) {
    this.defaultPeriod = defaultPeriod;
  }

  /**
   * Creates a new {@link Filter}.
   */
  public static Filter create() {
    return new Filter();
  }
}
