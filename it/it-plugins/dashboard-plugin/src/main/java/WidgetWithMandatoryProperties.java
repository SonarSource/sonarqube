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
import org.sonar.api.web.AbstractRubyTemplate;
import org.sonar.api.web.RubyRailsWidget;
import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;

@WidgetProperties({
  @WidgetProperty(key = "mandatoryString", optional = false),
  @WidgetProperty(key = "mandatoryInt", optional = false, type = WidgetPropertyType.INTEGER)

})
public class WidgetWithMandatoryProperties extends AbstractRubyTemplate implements RubyRailsWidget {

  public String getId() {
    return "widget-with-mandatory-properties";
  }

  public String getTitle() {
    return "Widget with Mandatory Properties";
  }

  @Override
  protected String getTemplatePath() {
    return "/widgets/widget-with-mandatory-properties.html.erb";
  }
}

