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

import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.rule.RuleKey;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

import java.util.Collection;

/**
 * Searches for rules. This component is available by dependency injection. It must not
 * be extended by plugins.
 *
 * @since 4.2
 * @deprecated since 7.4 we plan to stop loading rules on scanner side
 */
@Deprecated
@ScannerSide
@Immutable
public interface Rules {

  /**
   * Get a rule by its key. Returns <code>null</code> if rule does not exist.
   */
  @CheckForNull
  Rule find(RuleKey key);

  Collection<Rule> findAll();

  /**
   * Get the rules of the given repository. Returns an empty collection if the
   * repository does not exist.
   */
  Collection<Rule> findByRepository(String repository);

  /**
   * @since 5.2
   */
  Collection<Rule> findByInternalKey(String repository, String internalKey);

}
