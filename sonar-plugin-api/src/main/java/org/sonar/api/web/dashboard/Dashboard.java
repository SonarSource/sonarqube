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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
  private ListMultimap<Integer, Widget> widgetsByColumn = ArrayListMultimap.create();

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
   * The id is deduced from the name.
   */
  public static Dashboard createByName(String name) {
    String id = StringUtils.trimToEmpty(name);
    id = StringUtils.lowerCase(id);
    id = StringUtils.replaceChars(id, ' ', '_');
    return new Dashboard()
      .setId(id)
      .setName(name);
  }

  /**
   * Add a widget with the given parameters, and return the newly created {@link Widget} object if one wants to add parameters to it.
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
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("Dashboard id can not be blank");
    }
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

  public Dashboard setLayout(DashboardLayout dl) {
    if (dl == null) {
      throw new IllegalArgumentException("The layout of the dashboard '" + getId() + "' can not be null");
    }
    this.layout = dl;
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
      this.properties = Maps.newHashMap();
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
