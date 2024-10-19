/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectexport.rule;

import java.util.Collection;
import org.sonar.api.rule.RuleKey;

/**
 * This repository is responsible to register ids for {@link RuleKey}s and keeping track of them so that it can return
 * all of them in {@link #getAll()}.
 */
public interface RuleRepository {

  /**
   * Register the specified ref for the specified ruleKey and return it's representing {@link Rule} object.
   *
   * @throws IllegalArgumentException if the specified ruleKey is not the same as the one already in the repository (if any)
   */
  Rule register(String ref, RuleKey ruleKey);

  Collection<Rule> getAll();

}
