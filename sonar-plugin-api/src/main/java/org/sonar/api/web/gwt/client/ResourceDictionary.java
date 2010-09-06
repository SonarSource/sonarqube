/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.web.gwt.client;

import com.google.gwt.i18n.client.Dictionary;

public final class ResourceDictionary {

  public final static String CONF_PERMALINK_BASE = "permalink_url_base";
  public final static String CONF_RESOURCE_KEY = "resource_key";
  public final static String CONF_V_RESOURCE_KEY = "viewer_resource_key";
  public final static String CONF_V_PLUGIN_KEY = "viewer_plugin_key";
  public final static String CONF_V_METRIC_KEY = "metric";

  private ResourceDictionary() {
  }

  public static String getPermaLinkURLBase() {
    return Utils.getConfiguration(CONF_PERMALINK_BASE);
  }

  public static String getResourceKey() {
    return Utils.getConfiguration(CONF_RESOURCE_KEY);
  }

  public static String getViewerResourceKey() {
    return Utils.getConfiguration(CONF_V_RESOURCE_KEY);
  }

  public static String getViewerPluginKey() {
    return Utils.getConfiguration(CONF_V_PLUGIN_KEY);
  }

  public static String getViewerMetricKey() {
    return Utils.getConfiguration(CONF_V_METRIC_KEY);
  }

  public static Dictionary getResourceViewers() {
    return Dictionary.getDictionary("resource_viewers");
  }

}
