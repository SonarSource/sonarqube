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
package org.sonar.api.batch.rule;

import org.sonar.api.batch.ScannerSide;
import org.sonar.api.rule.RuleKey;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

import java.util.Collection;

/**
 * The rules that are activated on the current module. Quality profiles are
 * merged, so rules can relate to different repositories and languages.
 * <br>
 * Use {@link org.sonar.api.batch.rule.internal.ActiveRulesBuilder} to instantiate
 * this component in unit tests.
 *
 * @since 4.2
 */
@Immutable
@ScannerSide
public interface ActiveRules {

  /**
   * Find a {@link ActiveRule} by the associated rule key. <code>null</code>
   * is returned if the rule does not exist or if the rule is not activated
   * on any Quality profile associated with the module.
   */
  @CheckForNull
  ActiveRule find(RuleKey ruleKey);

  /**
   * All the active rules, whatever their repository and related language.
   */
  Collection<ActiveRule> findAll();

  /**
   * The active rules for a given repository, like <code>findbugs</code>
   */
  Collection<ActiveRule> findByRepository(String repository);

  /**
   * The active rules for a given language, like <code>java</code>
   */
  Collection<ActiveRule> findByLanguage(String language);

  /**
   * Find a {@link ActiveRule} by the associated internal key. <code>null</code>
   * is returned if the rule does not exist or if the rule is not activated
   * on any Quality profile associated with the module.
   */
  @CheckForNull
  ActiveRule findByInternalKey(String repository, String internalKey);

}
