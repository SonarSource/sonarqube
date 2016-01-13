/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
  @WidgetProperty(key = "chartTitle", type = WidgetPropertyType.STRING),
  @WidgetProperty(key = "chartHeight", type = WidgetPropertyType.INTEGER, defaultValue = "300"),
  @WidgetProperty(key = "filter", type = WidgetPropertyType.FILTER, optional = false),
  @WidgetProperty(key = "xMetric", type = WidgetPropertyType.METRIC, defaultValue = CoreMetrics.NCLOC_KEY, options = {WidgetConstants.FILTER_OUT_NEW_METRICS}),
  @WidgetProperty(key = "yMetric", type = WidgetPropertyType.METRIC, defaultValue = CoreMetrics.VIOLATIONS_KEY, options = {WidgetConstants.FILTER_OUT_NEW_METRICS}),
  @WidgetProperty(key = "sizeMetric", type = WidgetPropertyType.METRIC, defaultValue = CoreMetrics.TECHNICAL_DEBT_KEY, options = {WidgetConstants.FILTER_OUT_NEW_METRICS}),
  @WidgetProperty(key = "xLogarithmic", type = WidgetPropertyType.BOOLEAN),
  @WidgetProperty(key = "yLogarithmic", type = WidgetPropertyType.BOOLEAN),
  @WidgetProperty(key = "maxItems", type = WidgetPropertyType.INTEGER)
})
public class MeasureFilterAsBubbleChartWidget extends CoreWidget {

  public MeasureFilterAsBubbleChartWidget() {
    super("measure_filter_bubble_chart", "Measure Filter as Bubble Chart", "/org/sonar/server/dashboard/widget/measure_filter_bubble_chart.html.erb");
  }

}
