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
package org.sonar.plugins.core.widgets.measures;

import org.sonar.api.web.*;
import org.sonar.plugins.core.widgets.CoreWidget;

import static org.sonar.api.web.WidgetScope.GLOBAL;

@WidgetCategory({"Filters"})
@WidgetScope(GLOBAL)
@WidgetProperties({
  @WidgetProperty(key = MeasureFilterTreemapWidget.FILTER_PROPERTY, type = WidgetPropertyType.FILTER, optional = false),
  @WidgetProperty(key = MeasureFilterTreemapWidget.SIZE_METRIC_PROPERTY, type = WidgetPropertyType.METRIC, optional = true),
  @WidgetProperty(key = MeasureFilterTreemapWidget.COLOR_METRIC_PROPERTY, type = WidgetPropertyType.METRIC, optional = true, options = "type:PERCENT"),
  @WidgetProperty(key = MeasureFilterTreemapWidget.HEIGHT_PERCENTS_PROPERTY, type = WidgetPropertyType.INTEGER, optional = true, defaultValue = "55",
    description = "Height in percents of width"),
  @WidgetProperty(key = MeasureFilterListWidget.DISPLAY_FILTER_DESCRIPTION, type = WidgetPropertyType.BOOLEAN, defaultValue = "false")
})
public class MeasureFilterTreemapWidget extends CoreWidget {
  public static final String FILTER_PROPERTY = "filter";
  public static final String SIZE_METRIC_PROPERTY = "sizeMetric";
  public static final String COLOR_METRIC_PROPERTY = "colorMetric";
  public static final String HEIGHT_PERCENTS_PROPERTY = "heightInPercents";
  public static final String DISPLAY_FILTER_DESCRIPTION = "displayFilterDescription";
  public static final String ID = "measure_filter_treemap";

  public MeasureFilterTreemapWidget() {
    super(ID, "Measure Filter as Treemap", "/org/sonar/plugins/core/widgets/measure_filter_treemap.html.erb");
  }
}
