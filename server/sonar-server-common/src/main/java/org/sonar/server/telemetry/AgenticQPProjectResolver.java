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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ProjectQProfileLanguageAssociationDto;
import org.sonar.db.qualityprofile.QProfileDto;

import static org.sonar.server.telemetry.TelemetryAgenticQPAdoptionProvider.AGENTIC_QUALITY_PROFILE_NAME;

@ServerSide
public class AgenticQPProjectResolver {

  private final DbClient dbClient;

  public AgenticQPProjectResolver(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Returns a map of language to project UUIDs whose effective quality profile for that language
   * is the agentic quality profile. Resolves both explicit project assignments and the default
   * profile per language. Returns an empty map if the agentic profile does not exist.
   * 
   * For default profiles: only projects that have been analyzed (have explicit profiles for
   * at least one language) are included. This avoids counting unanalyzed projects that merely
   * inherit defaults. 
   */
  public Map<String, Set<String>> resolveAgenticProjectUuidsByLanguage(DbSession dbSession) {
    List<QProfileDto> agenticProfiles = dbClient.qualityProfileDao().selectBuiltInByName(dbSession, AGENTIC_QUALITY_PROFILE_NAME);
    if (agenticProfiles.isEmpty()) {
      return Map.of();
    }

    Set<String> agenticProfileUuids = agenticProfiles.stream()
      .map(QProfileDto::getKee)
      .collect(Collectors.toSet());

    List<ProjectQProfileLanguageAssociationDto> allAssociations = dbClient.qualityProfileDao().selectAllProjectAssociations(dbSession);

    Map<String, Set<String>> result = new HashMap<>();
    for (ProjectQProfileLanguageAssociationDto assoc : allAssociations) {
      if (agenticProfileUuids.contains(assoc.profileKey())) {
        result.computeIfAbsent(assoc.language(), k -> new HashSet<>()).add(assoc.projectUuid());
      }
    }

    List<QProfileDto> defaultProfiles = dbClient.qualityProfileDao().selectAllDefaultProfiles(dbSession);
    Set<String> analyzedProjects = allAssociations.stream()
      .map(ProjectQProfileLanguageAssociationDto::projectUuid)
      .collect(Collectors.toSet());

    for (QProfileDto defaultProfile : defaultProfiles) {
      if (agenticProfileUuids.contains(defaultProfile.getKee())) {
        String language = defaultProfile.getLanguage();
        Set<String> projectsWithExplicitForLanguage = allAssociations.stream()
          .filter(a -> language.equals(a.language()))
          .map(ProjectQProfileLanguageAssociationDto::projectUuid)
          .collect(Collectors.toSet());

        analyzedProjects.stream()
          .filter(uuid -> !projectsWithExplicitForLanguage.contains(uuid))
          .forEach(uuid -> result.computeIfAbsent(language, k -> new HashSet<>()).add(uuid));
      }
    }

    return result;
  }
}
