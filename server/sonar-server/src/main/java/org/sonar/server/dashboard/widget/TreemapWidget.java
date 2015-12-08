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
import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;

@WidgetProperties({
  @WidgetProperty(key = "chartTitle", type = WidgetPropertyType.STRING),
  @WidgetProperty(key = "sizeMetric", type = WidgetPropertyType.METRIC, defaultValue = CoreMetrics.NCLOC_KEY,
    options = {WidgetConstants.FILTER_OUT_NEW_METRICS}),
  @WidgetProperty(key = "colorMetric", type = WidgetPropertyType.METRIC, defaultValue = CoreMetrics.COVERAGE_KEY,
    options = {WidgetConstants.FILTER_OUT_NEW_METRICS, "type:PERCENT,RATING,LEVEL"}),
  @WidgetProperty(key = "heightInPercents", type = WidgetPropertyType.INTEGER, defaultValue = "55", optional = true),
  @WidgetProperty(key = "maxItems", type = WidgetPropertyType.INTEGER, defaultValue = "100")
})
public class TreemapWidget extends CoreWidget {
  public TreemapWidget() {
    super("treemap-widget", "Treemap of Components", "/org/sonar/server/dashboard/widget/treemap.html.erb");
  }
}
