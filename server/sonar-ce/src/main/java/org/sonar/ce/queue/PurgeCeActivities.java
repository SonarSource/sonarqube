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
package org.sonar.ce.queue;

import org.sonar.api.Startable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.purge.PurgeProfiler;

@ComputeEngineSide
public class PurgeCeActivities implements Startable {

  private final DbClient dbClient;
  private final PurgeProfiler profiler;

  public PurgeCeActivities(DbClient dbClient, PurgeProfiler profiler) {
    this.dbClient = dbClient;
    this.profiler = profiler;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.purgeDao().purgeCeActivities(dbSession, profiler);
      dbClient.purgeDao().purgeCeScannerContexts(dbSession, profiler);
      dbSession.commit();
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
