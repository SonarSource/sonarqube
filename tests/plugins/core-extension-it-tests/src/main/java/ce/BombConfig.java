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
package ce;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

@ServerSide
@ComputeEngineSide
public class BombConfig {
  private static final String OOM_START_BOMB_KEY = "oomStartBomb";
  private static final String ISE_START_BOMB_KEY = "iseStartBomb";
  private static final String OOM_STOP_BOMB_KEY = "oomStopBomb";
  private static final String ISE_STOP_BOMB_KEY = "iseStopBomb";

  private final DbClient dbClient;

  public BombConfig(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public void reset() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.internalPropertiesDao().save(dbSession, OOM_START_BOMB_KEY, String.valueOf(false));
      dbClient.internalPropertiesDao().save(dbSession, ISE_START_BOMB_KEY, String.valueOf(false));
      dbClient.internalPropertiesDao().save(dbSession, OOM_STOP_BOMB_KEY, String.valueOf(false));
      dbClient.internalPropertiesDao().save(dbSession, ISE_STOP_BOMB_KEY, String.valueOf(false));
      dbSession.commit();
    }
  }

  public boolean isOomStartBomb() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.internalPropertiesDao().selectByKey(dbSession, OOM_START_BOMB_KEY).map(Boolean::valueOf).orElse(false);
    }
  }

  public void setOomStartBomb(boolean oomStartBomb) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.internalPropertiesDao().save(dbSession, OOM_START_BOMB_KEY, String.valueOf(oomStartBomb));
      dbSession.commit();
    }
  }

  public boolean isIseStartBomb() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.internalPropertiesDao().selectByKey(dbSession, ISE_START_BOMB_KEY).map(Boolean::valueOf).orElse(false);
    }
  }

  public void setIseStartBomb(boolean iseStartBomb) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.internalPropertiesDao().save(dbSession, ISE_START_BOMB_KEY, String.valueOf(iseStartBomb));
      dbSession.commit();
    }
  }

  public boolean isOomStopBomb() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.internalPropertiesDao().selectByKey(dbSession, OOM_STOP_BOMB_KEY).map(Boolean::valueOf).orElse(false);
    }
  }

  public void setOomStopBomb(boolean oomStopBomb) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.internalPropertiesDao().save(dbSession, OOM_STOP_BOMB_KEY, String.valueOf(oomStopBomb));
      dbSession.commit();
    }
  }

  public boolean isIseStopBomb() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.internalPropertiesDao().selectByKey(dbSession, ISE_STOP_BOMB_KEY).map(Boolean::valueOf).orElse(false);
    }
  }

  public void setIseStopBomb(boolean iseStopBomb) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.internalPropertiesDao().save(dbSession, ISE_STOP_BOMB_KEY, String.valueOf(iseStopBomb));
      dbSession.commit();
    }
  }
}
