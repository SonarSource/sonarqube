/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.bootstrap;

import java.util.Map;
import org.sonar.api.Plugin;
import org.sonar.api.batch.BatchSide;
import org.sonar.core.platform.PluginInfo;

@BatchSide
public interface PluginInstaller {

  /**
   * Gets the list of plugins installed on server and downloads them if not
   * already in local cache.
   * @return information about all installed plugins, grouped by key
   */
  Map<String, PluginInfo> installRemotes();

  /**
   * Used only by tests.
   * @see org.sonar.batch.mediumtest.BatchMediumTester
   */
  Map<String, Plugin> installLocals();
}
