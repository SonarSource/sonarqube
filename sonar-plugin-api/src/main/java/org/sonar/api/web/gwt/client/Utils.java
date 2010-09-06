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
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import org.sonar.api.web.gwt.client.webservices.Resource;

import java.util.Set;

/**
 * A class of web utility
 *
 * @since 1.10
 */
public final class Utils {
  private Utils() {
  }

  public static String getConfiguration(String key) {
    return getConfiguration(key, null);
  }

  public static String getConfiguration(String key, String defaultValue) {
    String result = getDictionaryEntry("config", key);
    if (result == null) {
      result = defaultValue;
    }
    return result;
  }

  public static native void setConfiguration(String key, String val)  /*-{
    $wnd.config[key] = val;
  }-*/;

  public static String getRequestParameter(String key) {
    return getDictionaryEntry("request_parameters", key);
  }

  public static Set<String> getConfigurationKeys() {
    return getDictionaryKeys("config");
  }

  public static Set<String> getRequestParameterNames() {
    return getDictionaryKeys("request_parameters");
  }

  private static String getDictionaryEntry(String dictionaryName, String key) {
    try {
      Dictionary dic = Dictionary.getDictionary(dictionaryName);
      if (dic != null) {
        return dic.get(key);
      }
      return null;

    } catch (Exception e) {
      return null;
    }
  }

  private static Set<String> getDictionaryKeys(String dictionaryName) {
    Dictionary dic = Dictionary.getDictionary(dictionaryName);
    if (dic != null) {
      return dic.keySet();
    }
    return null;
  }

  public static String widgetGWTIdJSEncode(String widgetGWTId) {
    return widgetGWTId.replace('.', '_');
  }

  public static String getServerUrl() {
    return getConfiguration("sonar_url");
  }

  public static String getServerApiUrl() {
    return getServerUrl() + "/api";
  }

  public static String escapeHtml(String maybeHtml) {
    final Element div = DOM.createDiv();
    DOM.setInnerText(div, maybeHtml);
    return DOM.getInnerHTML(div);
  }

  public static String formatPercent(String percentage) {
    return percentage == null || percentage.equals("") ? "" : formatPercent(new Double(percentage));
  }

  public static String formatPercent(double percentage) {
    return NumberFormat.getFormat("0.0").format(percentage) + "%";
  }

  public static String formatNumber(String number) {
    return number == null || number.equals("") ? "" : formatNumber(new Double(number));
  }

  public static String formatNumber(double number) {
    return NumberFormat.getDecimalFormat().format(number);
  }

  public static native void showError(String message) /*-{
    $wnd.error(message);
  }-*/;

  public static native void showWarning(String message) /*-{
    $wnd.warning(message);
  }-*/;

  public static native void showInfo(String message) /*-{
    $wnd.info(message);
  }-*/;

  /**
   * Display the resource in a popup.
   *
   * @param resource  the resource to display, not null
   * @param metricKey the metric to highlight (optional : can be null)
   */
  public static void openResourcePopup(final Resource resource, final String metricKey) {
    String url = Utils.getServerUrl() + "/resource/index/" + resource.getId();
    if (metricKey != null) {
      url += "?" + ResourceDictionary.CONF_V_METRIC_KEY + "=" + metricKey;
    }
    Window.open(url, "resource", "height=800,width=900,scrollbars=1,resizable=1");
  }

  public static String getUrlToRuleDescription(final String ruleKey, final boolean showLayout) {
    return Utils.getServerUrl() + "/rules/show/" + ruleKey + "?layout=" + showLayout;
  }
}


