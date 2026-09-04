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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

@ServerSide
public class TelemetryCustomProfileActiveRulesProvider extends AbstractTelemetryDataProvider<String> {

  public static final String METRIC_KEY = "custom_profile_active_rule";
  private static final int MAX_CUSTOM_PROFILES = 100;

  private final DbClient dbClient;

  public TelemetryCustomProfileActiveRulesProvider(DbClient dbClient) {
    super(METRIC_KEY, Dimension.INSTALLATION, Granularity.WEEKLY, TelemetryDataType.STRING);
    this.dbClient = dbClient;
  }

  @Override
  public Map<String, String> getValues() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<QProfileDto> customProfiles = dbClient.qualityProfileDao().selectAll(dbSession).stream()
        .filter(profile -> !profile.isBuiltIn())
        .toList();

      Set<String> defaultProfileUuids = dbClient.qualityProfileDao().selectAllDefaultProfiles(dbSession).stream()
        .map(QProfileDto::getKee)
        .collect(Collectors.toSet());
      Map<String, Long> projectCountByProfileUuid = dbClient.qualityProfileDao().countProjectsByProfiles(dbSession, customProfiles);

      List<String> relevantProfileUuids = customProfiles.stream()
        .map(QProfileDto::getKee)
        .filter(uuid -> defaultProfileUuids.contains(uuid) || projectCountByProfileUuid.getOrDefault(uuid, 0L) > 0)
        .limit(MAX_CUSTOM_PROFILES)
        .toList();

      Map<String, String> result = new HashMap<>();
      DatabaseUtils.executeLargeInputs(relevantProfileUuids,
        uuids -> dbClient.activeRuleDao().selectByProfileUuids(dbSession, uuids))
        .stream()
        .filter(rule -> rule.getTemplateUuid() == null)
        .forEach(rule -> result.put(
          rule.getOrgProfileUuid() + "|" + rule.getRuleKey(),
          rule.getSeverityString()));
      return result;
    }
  }
}
