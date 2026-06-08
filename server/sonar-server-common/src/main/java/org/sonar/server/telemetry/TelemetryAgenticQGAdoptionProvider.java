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

import java.util.Optional;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

@ServerSide
public class TelemetryAgenticQGAdoptionProvider extends AbstractTelemetryDataProvider<Integer> {

  public static final String AGENTIC_QUALITY_GATE_NAME = "Sonar way for Agentic AI";
  public static final String METRIC_KEY = "agentic_qg_projects_count";

  private final DbClient dbClient;
  private final AgenticQGProjectResolver agenticQGProjectResolver;

  public TelemetryAgenticQGAdoptionProvider(DbClient dbClient, AgenticQGProjectResolver agenticQGProjectResolver) {
    super(METRIC_KEY, Dimension.INSTALLATION, Granularity.WEEKLY, TelemetryDataType.INTEGER);
    this.dbClient = dbClient;
    this.agenticQGProjectResolver = agenticQGProjectResolver;
  }

  @Override
  public Optional<Integer> getValue() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return Optional.of(agenticQGProjectResolver.resolveAgenticProjectUuids(dbSession).size());
    }
  }
}
