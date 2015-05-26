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
package org.sonar.server.dashboard.widget;

import org.sonar.api.web.WidgetCategory;
import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;
import org.sonar.api.web.WidgetScope;

import static org.sonar.api.web.WidgetScope.GLOBAL;

@WidgetCategory({"Filters"})
@WidgetScope(GLOBAL)
@WidgetProperties({
  @WidgetProperty(key = MeasureFilterListWidget.FILTER_PROPERTY, type = WidgetPropertyType.FILTER, optional = false),
  @WidgetProperty(key = MeasureFilterListWidget.PAGE_SIZE_PROPERTY, type = WidgetPropertyType.INTEGER, defaultValue = "30"),
  @WidgetProperty(key = MeasureFilterListWidget.DISPLAY_FILTER_DESCRIPTION, type = WidgetPropertyType.BOOLEAN, defaultValue = "false")
})
public class MeasureFilterListWidget extends CoreWidget {
  public static final String FILTER_PROPERTY = "filter";
  public static final String PAGE_SIZE_PROPERTY = "pageSize";
  public static final String DISPLAY_FILTER_DESCRIPTION = "displayFilterDescription";
  public static final String ID = "measure_filter_list";

  public MeasureFilterListWidget() {
    super(ID, "Measure Filter as List", "/org/sonar/server/dashboard/widget/measure_filter_list.html.erb");
  }
}
