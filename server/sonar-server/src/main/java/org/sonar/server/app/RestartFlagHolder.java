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
package org.sonar.server.app;

/**
 * Holds a boolean flag representing the restarting status of the WebServer.
 * This boolean is {@code false} by default and can safely be changed concurrently using methods {@link #set()} and
 * {@link #unset()}.
 */
public interface RestartFlagHolder {
  /**
   * @return whether restarting flag has been set or not.
   */
  boolean isRestarting();

  /**
   * Sets the restarting flag to {@code true}, no matter it already is or not.
   */
  void set();

  /**
   * Sets the restarting flag to {@code false}, no matter it already is or not.
   */
  void unset();
}
