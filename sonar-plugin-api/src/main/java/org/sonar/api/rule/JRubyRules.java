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
package org.sonar.api.rule;

import org.sonar.api.ServerComponent;

/**
 * Facade for JRuby on Rails extensions to request rules.
 * <p>
 * Reference from Ruby code : <code>Api.rules</code>
 * </p>
 *
 * @since 3.6
 */
public interface JRubyRules extends ServerComponent {

  /**
   * Return the localized name of a rule.
   *
   * <p>
   *   Ruby: <code>Api.rules.ruleName(I18n.locale, rule.rule_key)</code>
   * </p>
   */
  String ruleName(String rubyLocale, RuleKey ruleKey);

}
