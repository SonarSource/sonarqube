/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.core.i18n;

import java.util.Locale;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.i18n.RuleI18n;
import org.sonar.api.rules.Rule;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

/**
 * @deprecated in 4.1. Rules are not localized anymore. See http://jira.sonarsource.com/browse/SONAR-4885
 */
@Deprecated
@ScannerSide
@ServerSide
@ComputeEngineSide
public class RuleI18nManager implements RuleI18n {

  private static final String NAME_SUFFIX = ".name";
  private static final String RULE_PREFIX = "rule.";

  private DefaultI18n defaultI18n;

  public RuleI18nManager(DefaultI18n defaultI18n) {
    this.defaultI18n = defaultI18n;
  }

  /**
   * @deprecated in 4.1. Rules are not localized anymore. See http://jira.sonarsource.com/browse/SONAR-4885
   */
  @Override
  @Deprecated
  public String getName(String repositoryKey, String ruleKey, Locale locale) {
    return getName(repositoryKey, ruleKey);
  }

  /**
   * @deprecated in 4.1. Rules are not localized anymore. See http://jira.sonarsource.com/browse/SONAR-4885
   */
  @Override
  @Deprecated
  public String getName(Rule rule, Locale locale) {
    return getName(rule);
  }

  /**
   * @deprecated in 4.1. Rules are not localized anymore. See http://jira.sonarsource.com/browse/SONAR-4885
   */
  @Override
  @Deprecated
  public String getDescription(String repositoryKey, String ruleKey, Locale locale) {
    return getDescription(repositoryKey, ruleKey);
  }

  /**
   * @deprecated in 4.1. Rules are not localized anymore. See http://jira.sonarsource.com/browse/SONAR-4885
   */
  @Override
  @Deprecated
  public String getParamDescription(String repositoryKey, String ruleKey, String paramKey, Locale locale) {
    return getParamDescription(repositoryKey, ruleKey, paramKey);
  }

  @Override
  @CheckForNull
  public String getName(String repositoryKey, String ruleKey) {
    return message(repositoryKey, ruleKey, NAME_SUFFIX);
  }

  @Override
  @CheckForNull
  public String getName(Rule rule) {
    String name = message(rule.getRepositoryKey(), rule.getKey(), NAME_SUFFIX);
    return name != null ? name : rule.getName();
  }

  @Override
  public String getDescription(String repositoryKey, String ruleKey) {
    String relatedProperty = new StringBuilder().append(RULE_PREFIX).append(repositoryKey).append(".").append(ruleKey).append(NAME_SUFFIX).toString();

    String ruleDescriptionFilePath = "rules/" + repositoryKey + "/" + ruleKey + ".html";
    String description = defaultI18n.messageFromFile(Locale.ENGLISH, ruleDescriptionFilePath, relatedProperty);
    if (description == null) {
      // Following line is to ensure backward compatibility (SONAR-3319)
      description = lookUpDescriptionInFormerLocation(ruleKey, relatedProperty);
    }
    return description;
  }

  /*
   * Method used to ensure backward compatibility for language plugins that store HTML rule description files in the former
   * location (which was used prior to Sonar 3.0).
   * 
   * See http://jira.sonarsource.com/browse/SONAR-3319
   */
  private String lookUpDescriptionInFormerLocation(String ruleKey, String relatedProperty) {
    return defaultI18n.messageFromFile(Locale.ENGLISH, ruleKey + ".html", relatedProperty);
  }

  @Override
  @CheckForNull
  public String getParamDescription(String repositoryKey, String ruleKey, String paramKey) {
    return message(repositoryKey, ruleKey, ".param." + paramKey);
  }

  @CheckForNull
  String message(String repositoryKey, String ruleKey, String suffix) {
    String propertyKey = new StringBuilder().append(RULE_PREFIX).append(repositoryKey).append(".").append(ruleKey).append(suffix).toString();
    return defaultI18n.message(Locale.ENGLISH, propertyKey, null);
  }

  static boolean isRuleProperty(String propertyKey) {
    return StringUtils.startsWith(propertyKey, RULE_PREFIX) && StringUtils.endsWith(propertyKey, NAME_SUFFIX) && !propertyKey.contains(".param.");
  }
}
