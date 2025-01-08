/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.container;

import org.sonar.ce.task.CeTask;
import org.sonar.core.platform.Container;
import org.sonar.core.platform.ExtensionContainer;

/**
 * The Compute Engine task container. Created for a specific parent {@link ExtensionContainer} and a specific {@link CeTask}.
 */
public interface TaskContainer extends Container, AutoCloseable {

  ExtensionContainer getParent();

  /**
   * Starts task container, starting any startable component in it.
   */
  void bootup();

  /**
   * Cleans up resources after process has been called and has returned.
   */
  @Override
  void close();
}
