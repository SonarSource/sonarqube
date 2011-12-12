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
package org.sonar.api.web;

/**
 * Possible layouts for a dashboard.
 * 
 * @since 2.13
 */
public class DashboardLayouts {

  public static final String ONE_COLUMN = "100%";
  public static final String TWO_COLUMNS = "50%-50%";
  public static final String TWO_COLUMNS_30_70 = "30%-70%";
  public static final String TWO_COLUMNS_70_30 = "70%-30%";
  public static final String TREE_COLUMNS = "33%-33%-33%";

}
