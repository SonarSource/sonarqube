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
package org.sonar.server.qualitygate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.ProjectQgateAssociationDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.builtin.SonarWayQualityGate;

import static java.lang.String.format;

public class QualityGateFinder {
  private final DbClient dbClient;

  public QualityGateFinder(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public QualityGateData getEffectiveQualityGate(DbSession dbSession, ProjectDto projectDto) {
    return getEffectiveQualityGate(dbSession, projectDto.getUuid());
  }

  public QualityGateData getEffectiveQualityGate(DbSession dbSession, String projectUuid) {
    Optional<QualityGateData> res = getQualityGateForProject(dbSession, projectUuid);
    if (res.isPresent()) {
      return res.get();
    }
    QualityGateDto defaultQualityGate = getDefault(dbSession);
    return new QualityGateData(defaultQualityGate, true);
  }

  public Map<String, QualityGateData> getEffectiveQualityGateForAllProjects(DbSession dbSession) {
    List<ProjectDto> allProjects = dbClient.projectDao().selectAll(dbSession);
    Map<String, String> qgUuidPerProjectUuid =
      dbClient.projectQgateAssociationDao().selectAll(dbSession).stream()
        .filter(qga -> qga.getGateUuid() != null)
        .collect(Collectors.toMap(ProjectQgateAssociationDto::getUuid, ProjectQgateAssociationDto::getGateUuid));
    Map<String, QualityGateDto> qualityGates =
      dbClient.qualityGateDao().selectAll(dbSession).stream().collect(Collectors.toMap(QualityGateDto::getUuid, qg -> qg));

    Map<String, QualityGateData> qualityGatesPerProjectUuid =
      qgUuidPerProjectUuid.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
        e -> new QualityGateData(qualityGates.get(e.getValue()), false)));

    QualityGateData defaultQualityGateData = new QualityGateData(getDefault(dbSession), true);

    return allProjects.stream().collect(Collectors.toMap(EntityDto::getUuid,
      p -> Optional.ofNullable(qualityGatesPerProjectUuid.get(p.getUuid())).orElse(defaultQualityGateData)));
  }

  private Optional<QualityGateData> getQualityGateForProject(DbSession dbSession, String projectUuid) {
    return dbClient.projectQgateAssociationDao().selectQGateUuidByProjectUuid(dbSession, projectUuid)
      .map(qualityGateUuid -> dbClient.qualityGateDao().selectByUuid(dbSession, qualityGateUuid))
      .map(qualityGateDto -> new QualityGateData(qualityGateDto, false));
  }

  public QualityGateDto getDefault(DbSession dbSession) {
    return Optional.ofNullable(dbClient.qualityGateDao().selectDefault(dbSession)).orElseThrow(() -> new IllegalStateException("Default quality gate is missing"));
  }

  public QualityGateDto getSonarWay(DbSession dbSession) {
    return Optional.ofNullable(dbClient.qualityGateDao().selectByName(dbSession, SonarWayQualityGate.NAME)).orElseThrow(() ->
      new IllegalStateException(format("%s quality gate is missing", SonarWayQualityGate.NAME)));
  }

  public static class QualityGateData {
    private final String uuid;
    private final String name;
    private final boolean isDefault;
    private final boolean builtIn;
    private final boolean aiCodeSupported;

    QualityGateData(QualityGateDto qualityGate, boolean isDefault) {
      this.uuid = qualityGate.getUuid();
      this.name = qualityGate.getName();
      this.isDefault = isDefault;
      this.builtIn = qualityGate.isBuiltIn();
      this.aiCodeSupported = qualityGate.isAiCodeSupported();
    }

    public boolean isBuiltIn() {
      return builtIn;
    }

    public String getUuid() {
      return uuid;
    }

    public String getName() {
      return name;
    }

    public boolean isDefault() {
      return isDefault;
    }

    public boolean isAiCodeSupported() {
      return aiCodeSupported;
    }
  }

}
