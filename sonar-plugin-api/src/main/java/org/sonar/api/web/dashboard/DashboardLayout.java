/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.api.web.dashboard;

/**
 * Possible layouts for a dashboard.
 * 
 * @since 2.13
 */
public enum DashboardLayout {

  /**
   * Only 1 column that take all the page
   */
  ONE_COLUMN("100%"),

  /**
   * 2 columns of the same width
   */
  TWO_COLUMNS("50%-50%"),

  /**
   * 2 columns with the first one smaller than the second
   */
  TWO_COLUMNS_30_70("30%-70%"),

  /**
   * 2 columns with the first one bigger than the second
   */
  TWO_COLUMNS_70_30("70%-30%"),

  /**
   * 3 columns of the same width
   */
  TREE_COLUMNS("33%-33%-33%");

  private String layout;

  private DashboardLayout(String layout) {
    this.layout = layout;
  }

  @Override
  public String toString() {
    return layout;
  }

}
