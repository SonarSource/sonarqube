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

import java.sql.SQLException;

/**
 * INSERT, UPDATE or DELETE
 */
public interface Upsert extends SqlStatement<Upsert> {
  /**
   * Prepare for next statement.
   * @return {@code true} if the buffer of batched requests has been sent and transaction
   * has been committed, else {@code false}.
   */
  boolean addBatch() throws SQLException;

  Upsert execute() throws SQLException;

  Upsert commit() throws SQLException;

  /**
   * Number of requests required before sending group of batched
   * requests and before committing. Default value is {@link UpsertImpl#MAX_BATCH_SIZE}
   */
  Upsert setBatchSize(int i);
}
