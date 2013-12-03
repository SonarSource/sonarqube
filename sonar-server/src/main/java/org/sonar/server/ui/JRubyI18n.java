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
package org.sonar.server.ui;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.i18n.I18n;
import org.sonar.core.i18n.GwtI18n;
import org.sonar.core.i18n.RuleI18nManager;

import javax.annotation.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * Bridge between JRuby webapp and Java I18n component
 */
public class JRubyI18n implements ServerComponent {

  private I18n i18n;
  private Map<String, Locale> localesByRubyKey = Maps.newHashMap();
  private RuleI18nManager ruleI18nManager;
  private GwtI18n gwtI18n;

  public JRubyI18n(I18n i18n, RuleI18nManager ruleI18nManager, GwtI18n gwtI18n) {
    this.i18n = i18n;
    this.ruleI18nManager = ruleI18nManager;
    this.gwtI18n = gwtI18n;
  }

  Locale getLocale(String rubyKey) {
    Locale locale = localesByRubyKey.get(rubyKey);
    if (locale == null) {
      locale = toLocale(rubyKey);
      localesByRubyKey.put(rubyKey, locale);
    }
    return locale;
  }

  Map<String, Locale> getLocalesByRubyKey() {
    return localesByRubyKey;
  }

  public static Locale toLocale(@Nullable String rubyKey) {
    Locale locale;
    if (rubyKey == null) {
      locale = Locale.ENGLISH;
    } else {
      String[] fields = StringUtils.split(rubyKey, "-");
      if (fields.length == 1) {
        locale = new Locale(fields[0]);
      } else {
        locale = new Locale(fields[0], fields[1]);
      }
    }
    return locale;
  }

  public String message(String rubyLocale, String key, String defaultValue, Object... parameters) {
    return StringUtils.defaultString(i18n.message(getLocale(rubyLocale), key, defaultValue, parameters), key);
  }

  public String getRuleName(String repositoryKey, String key) {
    return ruleI18nManager.getName(repositoryKey, key);
  }

  public String getRuleDescription(String repositoryKey, String key) {
    return ruleI18nManager.getDescription(repositoryKey, key);
  }

  public String getRuleParamDescription(String repositoryKey, String ruleKey, String paramKey) {
    return ruleI18nManager.getParamDescription(repositoryKey, ruleKey, paramKey);
  }

  public String getJsDictionnary(String rubyLocale) {
    return gwtI18n.getJsDictionnary(toLocale(rubyLocale));
  }
}
