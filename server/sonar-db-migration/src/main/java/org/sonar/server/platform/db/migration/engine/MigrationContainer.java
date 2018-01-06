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
package org.sonar.server.platform.db.migration.engine;

import org.sonar.core.platform.ContainerPopulator;

/**
 * A dedicated container used to run DB migrations where all components are lazily instantiated.
 * <p>
 *   As a new container will be created for each run of DB migrations, components in this container can safely be
 *   stateful.
 * </p>
 * <p>
 *   Lazy instantiation is convenient to instantiate {@link org.sonar.server.platform.db.migration.step.MigrationStep}
 *   classes only they really are to be executed.
 * </p>
 */
public interface MigrationContainer extends ContainerPopulator.Container {

  /**
   * Cleans up resources after migration has run.
   * <strong>This method must never fail.</strong>
   */
  void cleanup();
}
