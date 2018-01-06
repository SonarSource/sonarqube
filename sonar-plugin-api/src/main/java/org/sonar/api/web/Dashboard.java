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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Definition of a dashboard.
 * <br>
 * Its name and description can be retrieved using the i18n mechanism, using the keys "dashboard.&lt;id&gt;.name" and
 * "dashboard.&lt;id&gt;.description".
 *
 * @since 2.13
 * @deprecated since 6.2, this extension is ignored as dashboards have been removed
 */
@Deprecated
public final class Dashboard {

  private String description;
  private DashboardLayout layout = DashboardLayout.TWO_COLUMNS;
  private ListMultimap<Integer, Widget> widgetsByColumn = ArrayListMultimap.create();
  private boolean activated = true;
  private boolean global = false;

  private Dashboard() {
  }

  /**
   * Creates a new {@link Dashboard}.
   */
  public static Dashboard create() {
    return new Dashboard();
  }

  /**
   * Add a widget with the given parameters, and return the newly created {@link Widget} object if one wants to add parameters to it.
   *
   * <p>The widget ids are listed by the web service /api/widgets
   *
   * @param widgetId id of an existing widget
   * @param columnId column starts with 1. The widget is ignored if the column id does not match the layout.
   */
  public Widget addWidget(String widgetId, int columnId) {
    if (columnId < 1) {
      throw new IllegalArgumentException("Widget column starts with 1");
    }

    Widget widget = new Widget(widgetId);
    widgetsByColumn.put(columnId, widget);
    return widget;
  }

  public Collection<Widget> getWidgets() {
    return widgetsByColumn.values();
  }

  public List<Widget> getWidgetsOfColumn(int columnId) {
    return widgetsByColumn.get(columnId);
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
   * <br>
   * Note: you should use the i18n mechanism for the description.
   *
   * @param description the description to set
   */
  public Dashboard setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Returns the layout. Default value is the 2 columns mode with width 50%/50%.
   *
   * @return the layout
   */
  public DashboardLayout getLayout() {
    return layout;
  }

  public Dashboard setLayout(DashboardLayout dl) {
    if (dl == null) {
      throw new IllegalArgumentException("The layout can not be null");
    }
    this.layout = dl;
    return this;
  }

  /**
   * A dashboard is global when it doesn't display information from a project but rather more general information.
   * <p>Before version 3.1 no dashboard was global.
   * <p>Since version 6.1, project dashboards are dropped. Project dashboards (global=false) are ignored.</p>
   *
   * @since 3.1
   */
  public boolean isGlobal() {
    return global;
  }

  /**
   * A dashboard is global when it doesn't display information from a project but rather more general information.
   * <p>Before version 3.1 no dashboard was global.</p>
   * <p>Since version 6.1, project dashboards are dropped. Project dashboards (global=false) are ignored.</p>
   *
   * @since 3.1
   */
  public Dashboard setGlobal(boolean global) {
    this.global = global;
    return this;
  }

  /**
   * A dashboard can be activated for all anonymous users or users who haven't configured their own dashboards.
   * <p>Before version 3.1 every dashboard created through this extension point was automatically activated.
   * This is still the default behavior.
   *
   * @since 3.1
   */
  public boolean isActivated() {
    return activated;
  }

  /**
   * Set whether the dashboard is activated for all anonymous users or users who haven't configured their own dashboards.
   *
   * @since 3.1
   */
  public Dashboard setActivated(boolean activated) {
    this.activated = activated;
    return this;
  }

  /**
   * Note that this class is an inner class to avoid confusion with the extension point org.sonar.api.web.Widget.
   */
  public static final class Widget {
    private String id;
    private Map<String, String> properties;

    Widget(String id) {
      this.id = id;
      this.properties = new HashMap<>();
    }

    public Widget setProperty(String key, String value) {
      properties.put(key, value);
      return this;
    }

    /**
     * Returns the properties of this widget.
     *
     * @return the properties
     */
    public Map<String, String> getProperties() {
      return properties;
    }

    public String getProperty(String key) {
      return properties.get(key);
    }

    /**
     * Returns the identifier of this widget.
     *
     * @return the id
     */
    public String getId() {
      return id;
    }
  }
}
