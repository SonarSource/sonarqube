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
package org.sonar.server.platform.db.migration.step;

import java.util.List;

/**
 * Responsible for:
 * <ul>
 *   <li>looping over all the {@link MigrationStep} to execute</li>
 *   <li>put INFO log between each {@link MigrationStep} for user information</li>
 *   <li>handle errors during the execution of {@link MigrationStep}</li>
 *   <li>update the content of table {@code SCHEMA_MIGRATION}</li>
 * </ul>
 */
public interface MigrationStepsExecutor {
  /**
   * @throws MigrationStepExecutionException at the first failing migration step execution
   */
  void execute(List<RegisteredMigrationStep> steps);
}
