/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.Calendar;
import java.util.Set;
import org.sonar.api.Startable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;

@ComputeEngineSide
public class PurgeCeActivities implements Startable {

  private static final Logger LOGGER = Loggers.get(PurgeCeActivities.class);

  private final DbClient dbClient;
  private final System2 system2;

  public PurgeCeActivities(DbClient dbClient, System2 system2) {
    this.dbClient = dbClient;
    this.system2 = system2;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Calendar sixMonthsAgo = Calendar.getInstance();
      sixMonthsAgo.setTimeInMillis(system2.now());
      sixMonthsAgo.add(Calendar.DATE, -180);

      LOGGER.info("Delete the Compute Engine tasks created before {}", sixMonthsAgo.getTime());
      Set<String> ceActivityUuids = dbClient.ceActivityDao().selectOlderThan(dbSession, sixMonthsAgo.getTimeInMillis())
        .stream()
        .map(CeActivityDto::getUuid)
        .collect(MoreCollectors.toSet());
      dbClient.ceActivityDao().deleteByUuids(dbSession, ceActivityUuids);
      dbClient.ceScannerContextDao().deleteByUuids(dbSession, ceActivityUuids);
      dbSession.commit();
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
