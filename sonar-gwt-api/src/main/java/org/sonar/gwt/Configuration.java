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
package org.sonar.gwt;

import com.google.gwt.i18n.client.Dictionary;

import java.util.Set;

public final class Configuration {

  private Configuration() {
    // only static methods
  }

  public static String getSonarVersion() {
    return getParameter("version");
  }

  /**
   * Get the id of the selected resource. Can be null if none resource is selected.
   */
  public static String getResourceId() {
    return getParameter("resource_key");
  }

  public static String getParameter(String key) {
    return getParameter(key, null);
  }

  public static String getParameter(String key, String defaultValue) {
    String result = getDictionaryEntry("config", key);
    if (result == null) {
      result = defaultValue;
    }
    return result;
  }

  public static native void setParameter(String key, String val)  /*-{
    $wnd.config[key] = val;
  }-*/;

  public static String getRequestParameter(String key) {
    return getDictionaryEntry("rp", key);
  }

  public static String getRequestParameter(String key, String defaultValue) {
    String value = getRequestParameter(key);
    return (value!=null ? value : defaultValue);
  }

  public static Set<String> getParameterKeys() {
    return getDictionaryKeys("config");
  }

  public static Set<String> getRequestParameterKeys() {
    return getDictionaryKeys("rp");
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

}
