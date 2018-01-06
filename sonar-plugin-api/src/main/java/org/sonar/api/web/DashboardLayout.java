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
package org.sonar.api.web;

/**
 * Possible layouts for a dashboard.
 *
 * @since 2.13
 * @deprecated since 6.2, this extension is ignored as dashboards have been removed
 */
@Deprecated
public enum DashboardLayout {

  /**
   * Only 1 column that take all the page
   */
  ONE_COLUMN("100%", 1),

  /**
   * 2 columns of the same width
   */
  TWO_COLUMNS("50%-50%", 2),

  /**
   * 2 columns with the first one smaller than the second
   */
  TWO_COLUMNS_30_70("30%-70%", 2),

  /**
   * 2 columns with the first one bigger than the second
   */
  TWO_COLUMNS_70_30("70%-30%", 2),

  /**
   * 3 columns of the same width
   */
  TREE_COLUMNS("33%-33%-33%", 3);

  private String code;
  private int columns;

  DashboardLayout(String code, int columns) {
    this.code = code;
    this.columns = columns;
  }

  public String getCode() {
    return code;
  }

  public int getColumns() {
    return columns;
  }

  @Override
  public String toString() {
    return code;
  }

}
