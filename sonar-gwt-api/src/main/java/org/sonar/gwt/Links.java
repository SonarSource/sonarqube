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
package org.sonar.gwt;

import com.google.gwt.user.client.Window;

public final class Links {

  public static final String DEFAULT_POPUP_HTML_FEATURES = "height=800,width=900,scrollbars=1,resizable=1";

  private Links() {
    // only static methods
  }

  public static String baseUrl() {
    return Configuration.getParameter("sonar_url");
  }

  public static String apiUrl() {
    return baseUrl() + "/api";
  }


  public static String urlForResource(String resourceIdOrKey) {
    return urlForMeasure(resourceIdOrKey, null);
  }

  public static String urlForMeasure(String resourceIdOrKey, String metricKey) {
    String url = baseUrl() + "/resource/index/" + resourceIdOrKey;
    if (metricKey != null) {
      url += "?metric=" + metricKey;
    }
    return url;
  }
  
  /**
   *
   * @param resourceIdOrKey
   * @param pageId
   * @param query additional query parameters. Can be null. Example "layout=false&param1=val1"
   */
  public static String urlForResourcePage(String resourceIdOrKey, String pageId, String query) {
    String url = baseUrl() + "/plugins/resource/";
    if (resourceIdOrKey != null) {
      url += resourceIdOrKey;
    }
    url += "?page=";
    url += pageId;
    if (query != null) {
      url += "&";
      url += query;
    }
    return url;
  }

  public static String urlForRule(String ruleIdOrKey, boolean showLayout) {
    return baseUrl() + "/rules/show/" + ruleIdOrKey + "?layout=" + showLayout;
  }

  public static String urlForDrilldown(String resourceIdOrKey, String metricKey) {
    return baseUrl() + "/drilldown/measures/" + resourceIdOrKey + "?metric=" + metricKey;
  }

  public static void openResourcePopup(final String resourceIdOrKey) {
    openMeasurePopup(resourceIdOrKey, null);
  }

  /**
   * Open the resource in a popup with HTML features like: height=800,width=900,scrollbars=1,resizable=1
   *
   * @param resourceIdOrKey the id or key of the resource to display, not null
   * @param metricKey       the metric to highlight (optional : can be null)
   */
  public static void openMeasurePopup(final String resourceIdOrKey, final String metricKey) {
    openMeasurePopup(resourceIdOrKey, metricKey, DEFAULT_POPUP_HTML_FEATURES);
  }


  public static void openMeasurePopup(final String resourceKey, final String metricKey, final String htmlFeatures) {
    String url = urlForMeasure(resourceKey, metricKey);
    Window.open(url, "resource", htmlFeatures);
  }


  public static void openResourcePage(final String pageId, final String resourceIdOrKey, final String query) {
    String url = urlForResourcePage(pageId, resourceIdOrKey, query);
    Window.Location.assign(url);
  }
}
