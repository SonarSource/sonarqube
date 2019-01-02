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
package org.sonar.api.batch.rule;

import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

import org.sonar.api.rule.RuleKey;

/**
 * Configuration of a rule activated on a Quality profile
 * @since 4.2
 */
@Immutable
public interface ActiveRule {

  RuleKey ruleKey();

  /**
   * Non-null severity.
   * @see org.sonar.api.rule.Severity
   */
  String severity();

  /**
   * Language of rule, for example <code>java</code>
   */
  String language();

  /**
   * Value of given parameter. Returns <code>null</code> if the parameter key does not
   * exist on the rule or if the parameter has no value nor default value.
   */
  @CheckForNull
  String param(String key);

  /**
   * Immutable parameter values. Returns an empty map if no parameters are defined.
   */
  Map<String, String> params();

  /**
   * Optional key declared and used by the underlying rule engine. As an example
   * the key of a Checkstyle rule looks like <code>com.puppycrawl.tools.checkstyle.checks.FooCheck</code>
   * whereas its internal key can be <code>Checker/TreeWalker/Foo</code>.
   */
  @CheckForNull
  String internalKey();

  /**
   * Optional rule key of the template rule.
   * @since 4.5.3
   */
  @CheckForNull
  String templateRuleKey();

  /**
   * Key of the quality profile the rule belongs to.
   * @since 7.5
   */
  String qpKey();
}
