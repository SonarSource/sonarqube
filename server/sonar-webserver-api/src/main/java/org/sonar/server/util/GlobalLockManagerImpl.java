/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static org.sonar.api.utils.Preconditions.checkArgument;

/**
 * Provide a simple mechanism to manage global locks across multiple nodes running in a cluster.
 * In the target use case multiple nodes try to execute something at around the same time,
 * and only the first should succeed, and the rest do nothing.
 */
@ComputeEngineSide
@ServerSide
public class GlobalLockManagerImpl implements GlobalLockManager {

  private final DbClient dbClient;

  public GlobalLockManagerImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public boolean tryLock(String name) {
    return tryLock(name, DEFAULT_LOCK_DURATION_SECONDS);
  }

  @Override
  public boolean tryLock(String name, int durationSecond) {
    checkArgument(
      !name.isEmpty() && name.length() <= LOCK_NAME_MAX_LENGTH,
      "name's length must be > 0 and <= %s: '%s'", LOCK_NAME_MAX_LENGTH, name);
    checkArgument(durationSecond > 0, "duration must be > 0: %s", durationSecond);

    try (DbSession dbSession = dbClient.openSession(false)) {
      boolean success = dbClient.internalPropertiesDao().tryLock(dbSession, name, durationSecond);
      dbSession.commit();
      return success;
    }
  }
}
