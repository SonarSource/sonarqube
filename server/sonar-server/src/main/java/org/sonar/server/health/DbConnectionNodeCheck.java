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
package org.sonar.server.health;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.IsAliveMapper;

import static org.sonar.server.health.Health.newHealthCheckBuilder;

/**
 * Checks Web Server can connect to the Database.
 */
public class DbConnectionNodeCheck implements NodeHealthCheck {
  private static final Logger LOGGER = Loggers.get(DbConnectionNodeCheck.class);
  private static final Health RED_HEALTH = newHealthCheckBuilder().setStatus(Health.Status.RED).addCause("Can't connect to DB").build();

  private final DbClient dbClient;

  public DbConnectionNodeCheck(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public Health check() {
    if (isConnectedToDB()) {
      return Health.GREEN;
    }
    return RED_HEALTH;
  }

  private boolean isConnectedToDB() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbSession.getMapper(IsAliveMapper.class).isAlive() == IsAliveMapper.IS_ALIVE_RETURNED_VALUE;
    } catch (RuntimeException e) {
      LOGGER.trace("DB connection is down: {}", e);
      return false;
    }
  }
}
