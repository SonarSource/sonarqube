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

@WidgetCategory({"Hotspots"})
@WidgetProperties({
  @WidgetProperty(key = "title", type = WidgetPropertyType.STRING),
  @WidgetProperty(key = "metric", type = WidgetPropertyType.METRIC, defaultValue = "ncloc", options = {WidgetConstants.FILTER_OUT_NEW_METRICS}),
  @WidgetProperty(key = "numberOfLines", type = WidgetPropertyType.INTEGER, defaultValue = "5")
})
public class HotspotMetricWidget extends CoreWidget {
  public HotspotMetricWidget() {
    super("hotspot_metric", "Metric hotspot", "/org/sonar/server/dashboard/widget/hotspot_metric.html.erb");
  }
}
