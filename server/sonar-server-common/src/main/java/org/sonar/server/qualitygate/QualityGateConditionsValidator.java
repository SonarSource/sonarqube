/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.qualitygate;

import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.metric.StandardToMQRMetrics;

public class QualityGateConditionsValidator {

  private final DbClient dbClient;

  public QualityGateConditionsValidator(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public boolean hasConditionsMismatch(boolean isMQRModeEnabled) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, String> metricKeysByUuids = dbClient.metricDao().selectAll(dbSession).stream()
        .collect(Collectors.toMap(MetricDto::getUuid, MetricDto::getKey));

      return dbClient.gateConditionDao().selectAll(dbSession).stream()
        .anyMatch(c -> isMQRModeEnabled ? StandardToMQRMetrics.isStandardMetric(metricKeysByUuids.get(c.getMetricUuid()))
          : StandardToMQRMetrics.isMQRMetric(metricKeysByUuids.get(c.getMetricUuid())));
    }
  }

}
