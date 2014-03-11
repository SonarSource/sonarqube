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

import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;

@WidgetProperties({
  @WidgetProperty(key = "metric1", type = WidgetPropertyType.METRIC),
  @WidgetProperty(key = "metric2", type = WidgetPropertyType.METRIC),
  @WidgetProperty(key = "metric3", type = WidgetPropertyType.METRIC),
  @WidgetProperty(key = "metric4", type = WidgetPropertyType.METRIC),
  @WidgetProperty(key = "metric5", type = WidgetPropertyType.METRIC),
  @WidgetProperty(key = "metric6", type = WidgetPropertyType.METRIC),
  @WidgetProperty(key = "metric7", type = WidgetPropertyType.METRIC),
  @WidgetProperty(key = "metric8", type = WidgetPropertyType.METRIC),
  @WidgetProperty(key = "metric9", type = WidgetPropertyType.METRIC),
  @WidgetProperty(key = "metric10", type = WidgetPropertyType.METRIC)
})
public class CustomMeasuresWidget extends CoreWidget {
  public CustomMeasuresWidget() {
    super("custom_measures", "Custom Measures", "/org/sonar/plugins/core/widgets/custom_measures.html.erb");
  }
}
