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

import org.sonar.api.Startable;

/**
 * This class is responsible for ensuring at startup that the persistence of migration history is possible.
 * <p>
 *   Therefor, it will create the migration history table if it does not exist yet, update it if necessary and fail
 *   if any of the two previous operations fails.
 * </p>
 * <p>
 *   This class is intended to be present only in the WebServer and only the web server is the startup leader.
 * </p>
 */
public interface MigrationHistoryTable extends Startable {
  String NAME = "schema_migrations";

  /**
   * Ensures that the history of db migrations can be persisted to database:
   * <ul>
   *   <li>underlying table {@code SCHEMA_MIGRATIONS} is created if it does not exist</li>
   *   <li>underlying table {@code SCHEMA_MIGRATIONS} is updated if needed</li>
   * </ul>
   *
   * @throws IllegalStateException if we can not ensure that table {@code SCHEMA_MIGRATIONS} can be accessed correctly
   */
  @Override
  void start();
}
