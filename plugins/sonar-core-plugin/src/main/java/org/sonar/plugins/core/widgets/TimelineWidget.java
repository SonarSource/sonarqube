/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.core.widgets;

import org.sonar.api.web.AbstractRubyTemplate;
import org.sonar.api.web.RubyRailsWidget;
import org.sonar.api.web.WidgetCategory;
import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;

@WidgetCategory("History")
@WidgetProperties({
  @WidgetProperty(key = "chartTitle", type = WidgetPropertyType.STRING),
  @WidgetProperty(key = "metric1", type = WidgetPropertyType.METRIC, defaultValue = "ncloc", options = {"key:^(?!new_).*"}),
  @WidgetProperty(key = "metric2", type = WidgetPropertyType.METRIC, options = {"key:^(?!new_).*"}),
  @WidgetProperty(key = "metric3", type = WidgetPropertyType.METRIC, options = {"key:^(?!new_).*"}),
  @WidgetProperty(key = "hideEvents", type = WidgetPropertyType.BOOLEAN),
  @WidgetProperty(key = "chartHeight", type = WidgetPropertyType.INTEGER, defaultValue = "80")
})
public class TimelineWidget extends AbstractRubyTemplate implements RubyRailsWidget {
  public String getId() {
    return "timeline";
  }

  public String getTitle() {
    return "Timeline";
  }

  @Override
  protected String getTemplatePath() {
    return "/org/sonar/plugins/core/widgets/timeline.html.erb";
  }
}
