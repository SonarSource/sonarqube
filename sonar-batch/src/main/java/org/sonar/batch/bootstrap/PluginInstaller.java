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
package org.sonar.batch.bootstrap;

import org.sonar.api.BatchComponent;
import org.sonar.api.Plugin;
import org.sonar.core.platform.PluginInfo;

import java.util.Map;

public interface PluginInstaller extends BatchComponent {

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
