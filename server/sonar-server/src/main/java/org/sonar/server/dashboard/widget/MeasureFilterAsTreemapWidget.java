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

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.web.WidgetCategory;
import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;
import org.sonar.api.web.WidgetScope;

import static org.sonar.api.web.WidgetScope.GLOBAL;

@WidgetCategory({"Filters"})
@WidgetScope(GLOBAL)
@WidgetProperties({
  @WidgetProperty(key = MeasureFilterAsTreemapWidget.FILTER_PROPERTY, type = WidgetPropertyType.FILTER,
    optional = false),
  @WidgetProperty(key = MeasureFilterAsTreemapWidget.CHART_TITLE_PROPERTY, type = WidgetPropertyType.STRING),
  @WidgetProperty(key = MeasureFilterAsTreemapWidget.SIZE_METRIC_PROPERTY, type = WidgetPropertyType.METRIC,
    defaultValue = CoreMetrics.NCLOC_KEY, options = {WidgetConstants.FILTER_OUT_NEW_METRICS}),
  @WidgetProperty(key = MeasureFilterAsTreemapWidget.COLOR_METRIC_PROPERTY, type = WidgetPropertyType.METRIC,
    defaultValue = CoreMetrics.COVERAGE_KEY,
    options = {WidgetConstants.FILTER_OUT_NEW_METRICS, "type:PERCENT,RATING,LEVEL"}),
  @WidgetProperty(key = MeasureFilterAsTreemapWidget.HEIGHT_PERCENTS_PROPERTY, type = WidgetPropertyType.INTEGER,
    defaultValue = "55", description = "Height in percents of width", optional = true),
  @WidgetProperty(key = MeasureFilterAsTreemapWidget.MAX_ITEMS_PROPERTY, type = WidgetPropertyType.INTEGER,
    defaultValue = "30")
})
public class MeasureFilterAsTreemapWidget extends CoreWidget {
  public static final String FILTER_PROPERTY = "filter";
  public static final String SIZE_METRIC_PROPERTY = "sizeMetric";
  public static final String COLOR_METRIC_PROPERTY = "colorMetric";
  public static final String HEIGHT_PERCENTS_PROPERTY = "heightInPercents";
  public static final String CHART_TITLE_PROPERTY = "chartTitle";
  public static final String MAX_ITEMS_PROPERTY = "maxItems";
  public static final String ID = "measure_filter_treemap";

  public MeasureFilterAsTreemapWidget() {
    super(ID, "Measure Filter as Treemap", "/org/sonar/server/dashboard/widget/measure_filter_treemap.html.erb");
  }
}
