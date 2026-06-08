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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.ProjectQgateAssociationDto;
import org.sonar.db.qualitygate.QualityGateDto;

import static org.sonar.server.telemetry.TelemetryAgenticQGAdoptionProvider.AGENTIC_QUALITY_GATE_NAME;

@ServerSide
public class AgenticQGProjectResolver {

  private final DbClient dbClient;

  public AgenticQGProjectResolver(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Returns UUIDs of all projects whose effective quality gate is "Sonar way for Agentic AI",
   * resolving both explicit project assignments and the instance default gate.
   * Returns an empty set if the agentic gate does not exist.
   */
  public Set<String> resolveAgenticProjectUuids(DbSession dbSession) {
    QualityGateDto agenticGate = dbClient.qualityGateDao().selectByName(dbSession, AGENTIC_QUALITY_GATE_NAME);
    if (agenticGate == null) {
      return Set.of();
    }
    return resolveAgenticProjectUuids(dbSession, agenticGate.getUuid());
  }

  /**
   * Returns true if the effective quality gate for the given project is "Sonar way for Agentic AI".
   * Resolves explicit project assignment first, falls back to the instance default gate.
   */
  public boolean isAgenticQGProject(DbSession dbSession, String projectUuid) {
    QualityGateDto agenticGate = dbClient.qualityGateDao().selectByName(dbSession, AGENTIC_QUALITY_GATE_NAME);
    if (agenticGate == null) {
      return false;
    }
    QualityGateDto effectiveGate = dbClient.qualityGateDao().selectByProjectUuid(dbSession, projectUuid);
    if (effectiveGate == null) {
      effectiveGate = dbClient.qualityGateDao().selectDefault(dbSession);
    }
    return effectiveGate != null && agenticGate.getUuid().equals(effectiveGate.getUuid());
  }

  private Set<String> resolveAgenticProjectUuids(DbSession dbSession, String agenticGateUuid) {
    List<ProjectQgateAssociationDto> allAssociations = dbClient.projectQgateAssociationDao().selectAll(dbSession);

    Set<String> explicitAdoptions = allAssociations.stream()
      .filter(assoc -> agenticGateUuid.equals(assoc.getGateUuid()))
      .map(ProjectQgateAssociationDto::getUuid)
      .collect(Collectors.toSet());

    QualityGateDto defaultGate = dbClient.qualityGateDao().selectDefault(dbSession);
    if (defaultGate == null || !agenticGateUuid.equals(defaultGate.getUuid())) {
      return explicitAdoptions;
    }

    Set<String> projectsWithExplicitOverride = allAssociations.stream()
      .filter(assoc -> assoc.getGateUuid() != null)
      .map(ProjectQgateAssociationDto::getUuid)
      .collect(Collectors.toSet());

    Set<String> defaultAdoptions = dbClient.projectDao().selectProjects(dbSession).stream()
      .map(ProjectDto::getUuid)
      .filter(uuid -> !projectsWithExplicitOverride.contains(uuid))
      .collect(Collectors.toSet());

    explicitAdoptions.addAll(defaultAdoptions);
    return explicitAdoptions;
  }
}
