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
package org.sonar.server.util;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

/**
 * Provide a simple mechanism to manage global locks across multiple nodes running in a cluster.
 * In the target use case multiple nodes try to execute something at around the same time,
 * and only the first should succeed, and the rest do nothing.
 */
@ComputeEngineSide
@ServerSide
public class GlobalLockManager {

  static final int DEFAULT_LOCK_DURATION_SECONDS = 180;

  private final DbClient dbClient;

  public GlobalLockManager(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Try to acquire a lock on the given name in the default namespace,
   * using the generic locking mechanism of {@see org.sonar.db.property.InternalPropertiesDao}.
   */
  public boolean tryLock(String name) {
    return tryLock(name, DEFAULT_LOCK_DURATION_SECONDS);
  }

  public boolean tryLock(String name, int durationSecond) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      boolean success = dbClient.internalPropertiesDao().tryLock(dbSession, name, durationSecond);
      dbSession.commit();
      return success;
    }
  }
}
