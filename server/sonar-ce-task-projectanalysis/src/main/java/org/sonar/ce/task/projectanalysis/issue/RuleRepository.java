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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Optional;
import java.util.function.Supplier;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DbSession;

/**
 * Repository of every rule in DB (including manual rules) whichever their status.
 */
public interface RuleRepository {

  /**
   * @throws NullPointerException if {@code key} is {@code null}
   * @throws IllegalArgumentException when there is no Rule for the specified RuleKey in the repository
   */
  Rule getByKey(RuleKey key);

  /**
   * @throws IllegalArgumentException when there is no Rule for the specified RuleKey in the repository
   */
  Rule getById(int id);

  /**
   * @throws NullPointerException if {@code key} is {@code null}
   */
  Optional<Rule> findByKey(RuleKey key);

  Optional<Rule> findById(int id);

  void addOrUpdateAddHocRuleIfNeeded(RuleKey ruleKey, Supplier<NewAdHocRule> ruleSupplier);

  void saveOrUpdateAddHocRules(DbSession dbSession);

}
