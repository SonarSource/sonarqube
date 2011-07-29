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
package org.sonar.core.i18n;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;

import java.util.Locale;

public class RuleI18nManager implements ServerComponent {

  private I18nManager i18nManager;

  public RuleI18nManager(I18nManager i18nManager) {
    this.i18nManager = i18nManager;
  }

  public String getName(String repositoryKey, String ruleKey, Locale locale) {
    return message(repositoryKey, ruleKey, locale, ".name", ruleKey);
  }

  public String getDescription(String repositoryKey, String ruleKey, Locale locale) {
    String relatedProperty = "rule." + repositoryKey + "." + ruleKey + ".name";

    // TODO add cache
    String description = i18nManager.messageFromFile(locale, ruleKey + ".html", relatedProperty);
    if (description == null && !Locale.ENGLISH.equals(locale)) {
      description = i18nManager.messageFromFile(Locale.ENGLISH, ruleKey + ".html", relatedProperty);
    }
    return StringUtils.defaultString(description, "");
  }

  public String getParamDescription(String repositoryKey, String ruleKey, String paramKey, Locale locale) {
    return message(repositoryKey, ruleKey, locale, ".param." + paramKey, "");
  }

  private String message(String repositoryKey, String ruleKey, Locale locale, String suffix, String defaultValue) {
    String propertyKey = new StringBuilder().append("rule.").append(repositoryKey).append(".").append(ruleKey).append(suffix).toString();
    return i18nManager.message(locale, propertyKey, defaultValue);
  }

//  static class RuleKey {
//    private String repositoryKey;
//    private String key;
//
//    RuleKey(String repositoryKey, String key) {
//      this.repositoryKey = repositoryKey;
//      this.key = key;
//    }
//
//    public String getRepositoryKey() {
//      return repositoryKey;
//    }
//
//    public String getKey() {
//      return key;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//      if (this == o) return true;
//      if (o == null || getClass() != o.getClass()) return false;
//
//      RuleKey ruleKey = (RuleKey) o;
//
//      if (!key.equals(ruleKey.key)) return false;
//      if (!repositoryKey.equals(ruleKey.repositoryKey)) return false;
//
//      return true;
//    }
//
//    @Override
//    public int hashCode() {
//      int result = repositoryKey.hashCode();
//      result = 31 * result + key.hashCode();
//      return result;
//    }
//
//    @Override
//    public String toString() {
//      return new StringBuilder().append(repositoryKey).append(":").append(key).toString();
//    }
//  }
//
//  static class RuleMessages {
//    private String name;
//    private String description;
//
//    RuleMessages(String name, String description) {
//      this.name = name;
//      this.description = description;
//    }
//  }
}
