/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.compliance;

import io.sonarcloud.compliancereports.dao.ActiveRuleDao;
import io.sonarcloud.compliancereports.dao.AggregationType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;

import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES_KEY;

@ServerSide
public class SqrActiveRuleDao implements ActiveRuleDao {
  private final DbClient dbClient;

  public SqrActiveRuleDao(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public Set<String> getActiveRuleKeys(
    String aggregationId,
    AggregationType aggregationType
  ) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      final var qualityProfiles = getQualityProfiles(dbSession, aggregationId)
        .stream()
        .map(QualityProfile::getQpKey)
        .toList();

      return getActiveRules(dbSession, qualityProfiles)
        .stream()
        .map(OrgActiveRuleDto::getRuleKey)
        .map(String::valueOf)
        .collect(Collectors.toSet());
    }
  }

  private Collection<OrgActiveRuleDto> getActiveRules(
    DbSession dbSession,
    List<String> qualityProfiles
  ) {
    if (qualityProfiles.isEmpty()) {
      return List.of();
    }
    return dbClient
      .activeRuleDao()
      .selectByProfileUuids(dbSession, qualityProfiles);
  }

  private Set<QualityProfile> getQualityProfiles(
    DbSession dbSession,
    String projectUuid
  ) {
    return dbClient
      .measureDao()
      .selectByComponentUuid(dbSession, projectUuid)
      .map(m -> m.getString(QUALITY_PROFILES_KEY))
      .map(data -> QPMeasureData.fromJson(data).getProfiles())
      .orElse(Collections.emptySortedSet());
  }
}
