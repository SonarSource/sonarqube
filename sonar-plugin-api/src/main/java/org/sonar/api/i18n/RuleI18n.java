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
package org.sonar.api.i18n;

import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;

import java.util.Locale;

/**
 * {@link I18n}-companion component that provides translation facilities for rule names, descriptions and parameter names.
 * 
 * @since 3.2
 */
public interface RuleI18n extends ServerComponent, BatchComponent {

  /**
   * Returns the localized name of the rule identified by its repository key and rule key.
   * <br>
   * If the name is not found in the given locale, then the default name is returned (the English one).
   * As a rule must have a name (this is a constraint in Sonar), this method never returns null.
   *
   * @param repositoryKey the repository key
   * @param ruleKey the rule key
   * @param locale the locale to translate into
   * @return the translated name of the rule, or the default English one if the given locale is not supported
   */
  String getName(String repositoryKey, String ruleKey, Locale locale);

  /**
   * Returns the localized description of the rule identified by its repository key and rule key.
   * <br>
   * If the description is not found in the given locale, then the default description is returned (the English one).
   * As a rule must have a description (this is a constraint in Sonar), this method never returns null.
   *
   * @param repositoryKey the repository key
   * @param ruleKey the rule key
   * @param locale  the locale to translate into
   * @return the translated description of the rule, or the default English one if the given locale is not supported
   */
  String getDescription(String repositoryKey, String ruleKey, Locale locale);

  /**
   * Returns the localized name of the rule parameter identified by the rules's key and repository key, and by the parameter key.
   * <br>
   * If the name is not found in the given locale, then the English translation is searched and return if found. Otherwise,
   * this method returns null (= if no translation can be found).
   *
   * @param repositoryKey the repository key
   * @param ruleKey the rule key
   * @param paramKey the parameter key
   * @param locale the locale to translate into
   * @return the translated name of the rule parameter, or the default English one if the given locale is not supported, or null if
   *         no translation can be found.
   */
  String getParamDescription(String repositoryKey, String ruleKey, String paramKey, Locale locale);

}
