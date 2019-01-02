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
package org.sonar.api.platform;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

/**
 * @since 2.5
 */
@ServerSide
@ComputeEngineSide
public interface ServerUpgradeStatus {

  /**
   * Has the database been upgraded during the current startup ? Return false when {@link #isFreshInstall()} is true.
   */
  boolean isUpgraded();

  /**
   * Has the database been created from scratch during the current startup ?
   */
  boolean isFreshInstall();

  /**
   * The database version before the server startup. Returns a non-zero negative value if db created from scratch.
   */
  int getInitialDbVersion();

}
