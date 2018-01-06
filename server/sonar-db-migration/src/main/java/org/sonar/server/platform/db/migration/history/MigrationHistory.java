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
package org.sonar.server.platform.db.migration.history;

import java.util.Optional;
import org.sonar.api.Startable;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

/**
 * This class is responsible for providing methods to read and write information from the persisted
 * history.
 * <p>
 *   This class assumes the Migration History table exists (see {@link MigrationHistoryTable}) and will
 *   fail at startup (see {@link #start()}) if it doesn't.
 * </p>
 */
public interface MigrationHistory extends Startable {
  /**
   * @throws IllegalStateException if the Migration History table does not exist.
   */
  @Override
  void start();

  /**
   * Returns the last execute migration number according to the persistence information.
   *
   * @return a long >= 0 or empty if the migration history is empty.
   */
  Optional<Long> getLastMigrationNumber();

  /**
   * Saves in persisted migration history the fact that the specified {@link RegisteredMigrationStep} has
   * been successfully executed.
   *
   * @throws RuntimeException if the information can not be persisted (and committed).
   */
  void done(RegisteredMigrationStep dbMigration);

}
