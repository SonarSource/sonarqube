/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.api.rules;

import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;

import javax.annotation.CheckForNull;

import java.util.Collection;

/**
 * @since 2.3
 * @deprecated since 5.1. Use {@link ActiveRules} on batch side.
 */
@Deprecated
@BatchSide
@ServerSide
public interface RuleFinder {

  /**
   * @since 2.5
   * @deprecated since 4.4, Please use {@link #findByKey(org.sonar.api.rule.RuleKey)}}
   */
  @CheckForNull
  @Deprecated
  Rule findById(int ruleId);

  @CheckForNull
  Rule findByKey(String repositoryKey, String key);

  @CheckForNull
  Rule findByKey(RuleKey key);

  /**
   * @throw IllegalArgumentException if more than one result
   */
  @CheckForNull
  Rule find(RuleQuery query);

  Collection<Rule> findAll(RuleQuery query);

}
