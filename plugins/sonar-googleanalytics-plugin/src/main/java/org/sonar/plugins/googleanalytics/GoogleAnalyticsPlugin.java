/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.googleanalytics;

import org.sonar.api.*;

import java.util.ArrayList;
import java.util.List;

@Properties({
    @Property(
        key = CoreProperties.GOOGLE_ANALYTICS_ACCOUNT_PROPERTY,
        name = "Account key",
        description = "Example : UA-1234567-8")
})
public class GoogleAnalyticsPlugin implements Plugin {

  public String getKey() {
    return CoreProperties.GOOGLE_ANALYTICS_PLUGIN;
  }

  public String getName() {
    return "Google analytics";
  }

  public String getDescription() {
    return "Google analytics is a tool that collects data on the traffic of web sites and then, through a powerful interface, enables to get reporting, segmentation, chart, .. on the traffic. You can find more by going to the  <a href='http://www.google.com/analytics/'>Google analytics web site</a>.";
  }

  public List<Class<? extends Extension>> getExtensions() {
    List<Class<? extends Extension>> list = new ArrayList<Class<? extends Extension>>();
    list.add(GoogleAnalyticsWebFooter.class);
    return list;
  }

  @Override
  public String toString() {
    return getKey();
  }
}
