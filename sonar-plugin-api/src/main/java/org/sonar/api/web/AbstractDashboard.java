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
 * Class that every plugin predefined dashboard should extend (as long as implement the {@link Dashboard} interface).
 * 
 * @since 2.13
 */
public abstract class AbstractDashboard {

  private static final String TEMPLATE_TYPE = "DASHBOARD";

  /**
   * Returns the identifier of this template.
   * 
   * @see Template#getId()
   * @return
   */
  public abstract String getId();

  /**
   * Returns the name of the dashboard.
   * 
   * @return the name
   */
  public abstract String getName();

  /**
   * Returns the description of the dashboard.
   * 
   * @return the description
   */
  public String getDescription() {
    return "";
  }

  /**
   * Returns the layout for the dashboard.
   * 
   * @see DashboardLayouts for the possible values.
   * @return the layout
   */
  public String getLayout() {
    return DashboardLayouts.TWO_COLUMNS;
  }

  /**
   * Returns the kind of template.
   * 
   * @see Template#getType()
   * @return the type
   */
  public final String getType() {
    return TEMPLATE_TYPE;
  }

}
