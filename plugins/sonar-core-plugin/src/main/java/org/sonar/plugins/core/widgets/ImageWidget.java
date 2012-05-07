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
import org.sonar.api.web.WidgetGlobal;
import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;

@WidgetCategory("Misc")
@WidgetGlobal
@WidgetProperties(
{
  @WidgetProperty(key = "imageUrl", type = WidgetPropertyType.STRING, defaultValue = "http://www.sonarsource.org/wp-content/themes/sonarsource.org/images/sonar.png"),
  @WidgetProperty(key = "alt", type = WidgetPropertyType.STRING, defaultValue = "SonarSource"),
  @WidgetProperty(key = "link", type = WidgetPropertyType.STRING, defaultValue = "http://www.sonarsource.org"),
  @WidgetProperty(key = "width", type = WidgetPropertyType.INTEGER, defaultValue = "100"),
  @WidgetProperty(key = "height", type = WidgetPropertyType.INTEGER, defaultValue = "54")
})
public class ImageWidget extends AbstractRubyTemplate implements RubyRailsWidget {

  public String getId() {
    return "image";
  }

  public String getTitle() {
    return "Image";
  }

  @Override
  protected String getTemplatePath() {
    return "/org/sonar/plugins/core/widgets/image.html.erb";
  }
}
