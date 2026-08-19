/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.startup;

import java.util.HashMap;
import java.util.Map;
import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonarsource.history.server.db.HistoryDbClient;

/** Registers the metric keys used by measure history. */
@ServerSide
public class RegisterMeasureKeyMappings implements Startable {

  private final DbClient dbClient;
  private final HistoryDbClient historyDbClient;

  public RegisterMeasureKeyMappings(DbClient dbClient, HistoryDbClient historyDbClient) {
    this.dbClient = dbClient;
    this.historyDbClient = historyDbClient;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, String> metricKeyToType = new HashMap<>();
      for (MetricDto metric : dbClient.metricDao().selectAll(dbSession)) {
        metricKeyToType.put(metric.getKey(), metric.getValueType() != null ? metric.getValueType() : "STRING");
      }
      historyDbClient.measureKeyMappingRepository().getOrCreate(dbSession, metricKeyToType);
      dbSession.commit();
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
