/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.telemetry.deprecated;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.qualityprofile.QProfileComparison;

import static java.util.stream.Collectors.toMap;

public class QualityProfileDataProvider {

  private final DbClient dbClient;
  private final QProfileComparison qProfileComparison;

  public QualityProfileDataProvider(DbClient dbClient, QProfileComparison qProfileComparison) {
    this.dbClient = dbClient;
    this.qProfileComparison = qProfileComparison;
  }

  public List<TelemetryData.QualityProfile> retrieveQualityProfilesData() {
    try (DbSession dbSession = dbClient.openSession(false)) {

      Set<String> defaultProfileUuids = dbClient.qualityProfileDao().selectAllDefaultProfiles(dbSession)
        .stream().map(QProfileDto::getKee)
        .collect(Collectors.toSet());

      Map<String, QProfileDto> allProfileDtosByUuid = dbClient.qualityProfileDao().selectAll(dbSession)
        .stream()
        .collect(toMap(QProfileDto::getKee, p -> p));

      return allProfileDtosByUuid.entrySet().stream()
        .map(p -> mapQualityProfile(p.getValue(), allProfileDtosByUuid, defaultProfileUuids.contains(p.getKey()), dbSession))
        .toList();
    }
  }

  private TelemetryData.QualityProfile mapQualityProfile(QProfileDto profile, Map<String, QProfileDto> allProfileDtos, boolean isDefault, DbSession dbSession) {
    QProfileDto rootProfile = getRootProfile(profile.getKee(), allProfileDtos);
    Boolean isBuiltInRootParent;
    if (profile.isBuiltIn()) {
      isBuiltInRootParent = null;
    } else {
      isBuiltInRootParent = rootProfile.isBuiltIn() && !rootProfile.getKee().equals(profile.getKee());
    }

    Optional<QProfileComparison.QProfileComparisonResult> rulesComparison = Optional.of(profile)
      .filter(p -> isBuiltInRootParent != null && isBuiltInRootParent)
      .map(p -> qProfileComparison.compare(dbSession, rootProfile, profile));

    return new TelemetryData.QualityProfile(profile.getKee(),
      profile.getParentKee(),
      profile.getLanguage(),
      isDefault,
      profile.isBuiltIn(),
      isBuiltInRootParent,
      rulesComparison.map(c -> c.modified().size()).orElse(null),
      rulesComparison.map(c -> c.inRight().size()).orElse(null),
      rulesComparison.map(c -> c.inLeft().size()).orElse(null)
    );
  }

  public QProfileDto getRootProfile(String kee, Map<String, QProfileDto> allProfileDtos) {
    QProfileDto qProfileDto = allProfileDtos.get(kee);
    String parentKee = qProfileDto.getParentKee();
    if (parentKee != null) {
      return getRootProfile(parentKee, allProfileDtos);
    } else {
      return allProfileDtos.get(kee);
    }
  }
}
