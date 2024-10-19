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
package org.sonar.server.util;

import org.sonar.api.Startable;
import org.sonar.api.impl.utils.DefaultTempFolder;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.TempFolder;

@ServerSide
public class TempFolderCleaner implements Startable {

  private TempFolder defaultTempFolder;

  public TempFolderCleaner(TempFolder defaultTempFolder) {
    this.defaultTempFolder = defaultTempFolder;
  }

  /**
   * This method should not be renamed. It follows the naming convention
   * defined by IoC container.
   */
  @Override
  public void start() {
    // Nothing to do
  }

  /**
   * This method should not be renamed. It follows the naming convention
   * defined by IoC container.
   */
  @Override
  public void stop() {
    ((DefaultTempFolder) defaultTempFolder).clean();
  }
}
