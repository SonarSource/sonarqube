/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.IsAliveMapper;

/**
 * Checks Web Server can connect to the Database.
 */
public class DbConnectionNodeCheck implements NodeHealthCheck {
  private static final Logger LOGGER = LoggerFactory.getLogger(DbConnectionNodeCheck.class);
  private static final Health RED_HEALTH = Health.builder().setStatus(Health.Status.RED).addCause("Can't connect to DB").build();

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
      LOGGER.trace("DB connection is down: {}", e.getMessage(), e);
      return false;
    }
  }
}
