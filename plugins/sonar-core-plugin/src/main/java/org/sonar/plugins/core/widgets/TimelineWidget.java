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
package org.sonar.plugins.core.widgets;

import org.sonar.api.web.WidgetCategory;
import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;

@WidgetCategory("History")
@WidgetProperties({
    @WidgetProperty(key = "chartTitle", type = WidgetPropertyType.STRING),
    @WidgetProperty(key = "metric1", type = WidgetPropertyType.METRIC, defaultValue = "ncloc", options = {WidgetConstants.FILTER_OUT_NEW_METRICS}),
    @WidgetProperty(key = "metric2", type = WidgetPropertyType.METRIC, options = {WidgetConstants.FILTER_OUT_NEW_METRICS}),
    @WidgetProperty(key = "metric3", type = WidgetPropertyType.METRIC, options = {WidgetConstants.FILTER_OUT_NEW_METRICS}),
    @WidgetProperty(key = "hideEvents", type = WidgetPropertyType.BOOLEAN),
    @WidgetProperty(key = "chartHeight", type = WidgetPropertyType.INTEGER, defaultValue = "180")
})
public class TimelineWidget extends CoreWidget {
  public TimelineWidget() {
    super("timeline", "Timeline", "/org/sonar/plugins/core/widgets/timeline.html.erb");
  }
}
