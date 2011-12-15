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

import com.google.common.collect.Lists;

import java.util.Collection;

/**
 * Definition of a dashboard.
 * <p/>
 * Its name and description can be retrieved using the i18n mechanism, using the keys "dashboard.&lt;id&gt;.name" and
 * "dashboard.&lt;id&gt;.description".
 *
 * @since 2.13
 */
public final class Dashboard {

  private String id;
  private String name;
  private String description;
  private DashboardLayout layout = DashboardLayout.TWO_COLUMNS;
  private Collection<Widget> widgets = Lists.newArrayList();

  private Dashboard() {
  }

  /**
   * Creates a new {@link Dashboard}-
   */
  public static Dashboard create(String id, String name) {
    return new Dashboard()
      .setId(id)
      .setName(name);
  }

  /**
   * Add a widget with the given parameters, and return the newly created {@link Widget} object if one wants to add parameters to it.
   */
  public Widget addWidget(String id, int columnId, int rowId) {
    Widget widget = new Widget(id, columnId, rowId);
    widgets.add(widget);
    return widget;
  }

  /**
   * Returns the list of widgets.
   *
   * @return the widgets of this dashboard
   */
  public Collection<Widget> getWidgets() {
    return widgets;
  }

  /**
   * Returns the identifier of the dashboard.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  private Dashboard setId(String id) {
    this.id = id;
    return this;
  }

  /**
   * Returns the name of the dashboard.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  private Dashboard setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Returns the description of the dashboard.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of the dashboard.
   * <p/>
   * Note: you should use the i18n mechanism for the description.
   *
   * @param description the description to set
   */
  public Dashboard setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Returns the layout of the dashboard.
   *
   * @return the layout
   */
  public DashboardLayout getLayout() {
    return layout;
  }

  /**
   * @param layout the layout to set
   */
  public Dashboard setLayout(DashboardLayout layout) {
    this.layout = layout;
    return this;
  }

}
