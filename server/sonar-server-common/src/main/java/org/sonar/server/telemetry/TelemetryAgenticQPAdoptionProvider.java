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
package org.sonar.server.telemetry;

import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

@ServerSide
public class TelemetryAgenticQPAdoptionProvider extends AbstractTelemetryDataProvider<Integer> {

  public static final String AGENTIC_QUALITY_PROFILE_NAME = "Sonar agentic AI";
  public static final String METRIC_KEY = "agentic_qp_projects_count";

  private final DbClient dbClient;
  private final AgenticQPProjectResolver agenticQPProjectResolver;

  public TelemetryAgenticQPAdoptionProvider(DbClient dbClient, AgenticQPProjectResolver agenticQPProjectResolver) {
    super(METRIC_KEY, Dimension.LANGUAGE, Granularity.WEEKLY, TelemetryDataType.INTEGER);
    this.dbClient = dbClient;
    this.agenticQPProjectResolver = agenticQPProjectResolver;
  }

  @Override
  public Map<String, Integer> getValues() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return agenticQPProjectResolver.resolveAgenticProjectUuidsByLanguage(dbSession).entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
    }
  }
}
