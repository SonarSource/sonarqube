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
package org.sonar.server.rule;

import org.sonar.api.rules.RuleFinder;

/**
 * {@link RuleFinder} implementation that supports caching used by the Web Server.
 * <p>
 *   Caching is enabled right after loading of rules is done (see {@link RegisterRules}) and disabled
 *   once all startup tasks are done (see {@link org.sonar.server.platform.platformlevel.PlatformLevelStartup}).
 * </p>
 */
public interface WebServerRuleFinder extends RuleFinder {
  /**
   * Enable caching.
   */
  void startCaching();

  /**
   * Disable caching.
   */
  void stopCaching();
}
