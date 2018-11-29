/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.api.i18n;

import java.util.Locale;
import javax.annotation.CheckForNull;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.rules.Rule;
import org.sonar.api.server.ServerSide;

/**
 * {@link I18n}-companion component that provides translation facilities for rule names, descriptions and parameter names.
 * 
 * @since 3.2
 * @deprecated in 4.1. Rules are not localized anymore. See http://jira.sonarsource.com/browse/SONAR-4885
 */
@Deprecated
@ServerSide
@ComputeEngineSide
public interface RuleI18n {

  /**
   * Returns the localized name of the rule identified by its repository key and rule key.
   * <br>
   * If the name is not found in the given locale, then the default name is returned (the English one).
   * This method could return null if no default name found. This is the cause for instance the copies rules.
   *
   * @param repositoryKey the repository key
   * @param ruleKey the rule key
   * @param locale not used
   * @return the translated name of the rule, or the default English one if the given locale is not supported, or null
   * @deprecated since 4.1. Rules are not localized anymore. See http://jira.sonarsource.com/browse/SONAR-4885
   */
  @Deprecated
  @CheckForNull
  String getName(String repositoryKey, String ruleKey, Locale locale);

  /**
   * Returns the name of the rule identified by its repository key and rule key.
   * <br>
   * This method could return null if no default name found. This is the cause for instance the copies rules.
   *
   * @param repositoryKey the repository key
   * @param ruleKey the rule key
   * @return the nullable name of the rule
   * @since 4.1
   */
  @CheckForNull
  String getName(String repositoryKey, String ruleKey);

  /**
   * Returns the localized name or the name of the rule.
   * <br>
   * If the name is not found in the given locale, then the default name is returned (the English one).
   * It the default name is not found, then the rule name is returned.
   *
   * @param rule the rule
   * @param locale the locale to translate into
   * @return the translated name of the rule, or the default English one if the given locale is not supported, or the rule name.
   * @deprecated since 4.1. Rules are not localized anymore. See http://jira.sonarsource.com/browse/SONAR-4885
   */
  @Deprecated
  @CheckForNull
  String getName(Rule rule, Locale locale);

  /**
   * Returns the name of the rule.
   * <br>
   * It the default name is not found, then the rule name is returned.
   *
   * @param rule the rule
   * @return the nullable name of the rule
   * @since 4.1
   */
  @CheckForNull
  String getName(Rule rule);

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
   * @deprecated since 4.1. Rules are not localized anymore. See http://jira.sonarsource.com/browse/SONAR-4885
   */
  @Deprecated
  String getDescription(String repositoryKey, String ruleKey, Locale locale);

  /**
   * Returns the description of the rule identified by its repository key and rule key.
   * <br>
   * As a rule must have a description (this is a constraint in SonarQube), this method never returns null.
   *
   * @param repositoryKey the repository key
   * @param ruleKey the rule key
   * @return the description of the rule
   * @since 4.1
   */
  String getDescription(String repositoryKey, String ruleKey);

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
   * @deprecated since 4.1. Rules are not localized anymore. See http://jira.sonarsource.com/browse/SONAR-4885
   */
  @Deprecated
  @CheckForNull
  String getParamDescription(String repositoryKey, String ruleKey, String paramKey, Locale locale);

  /**
   * Returns the name of the rule parameter identified by the rules's key and repository key, and by the parameter key.
   *
   * @param repositoryKey the repository key
   * @param ruleKey the rule key
   * @param paramKey the parameter key
   * @return the nullable name of the rule parameter
   * @since 4.1
   */
  @CheckForNull
  String getParamDescription(String repositoryKey, String ruleKey, String paramKey);

}
