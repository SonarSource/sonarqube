/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.i18n;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.api.ServerComponent;

import java.util.*;

public class GwtI18n implements ServerComponent {
  public static final String GWT_BUNDLE = DefaultI18n.BUNDLE_PACKAGE + "gwt";

  private DefaultI18n manager;
  private String[] propertyKeys;

  public GwtI18n(DefaultI18n manager) {
    this.manager = manager;
  }

  public void start() {
    doStart(getBundle(Locale.ENGLISH));
  }

  void doStart(ResourceBundle englishBundle) {
    List<String> keys = Lists.newArrayList();
    Enumeration<String> enumeration = englishBundle.getKeys();
    while (enumeration.hasMoreElements()) {
      String propertyKey = enumeration.nextElement();
      keys.add(propertyKey);
    }
    propertyKeys = keys.toArray(new String[keys.size()]);
  }

  String[] getPropertyKeys() {
    return propertyKeys;
  }

  /**
   * Used by the JRuby on Rails application
   */
  public String getJsDictionnary(Locale locale) {
    ResourceBundle bundle = getBundle(locale);
    return getJsDictionnary(bundle);
  }

  String getJsDictionnary(ResourceBundle bundle) {
    StringBuilder js = new StringBuilder("var l10n = {");
    for (int index = 0; index < propertyKeys.length; index++) {
      String key = propertyKeys[index];
      String value = StringEscapeUtils.escapeJavaScript(bundle.getString(key));
      if (index > 0) {
        js.append(",");
      }
      js.append("\"").append(key).append("\": \"").append(value).append("\"");
    }
    js.append("};");
    return js.toString();
  }

  ResourceBundle getBundle(Locale locale) {
    try {
      return ResourceBundle.getBundle(GWT_BUNDLE, locale, manager.getBundleClassLoader());
    } catch (MissingResourceException e) {
      throw new IllegalStateException("The English bundle for GWT extensions is not deployed", e);
    }
  }
}
