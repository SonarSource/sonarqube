/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.Optional;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateDto;

import static com.google.common.base.Preconditions.checkState;

public class QualityGateFinder {
  private static final String DEFAULT_QUALITY_GATE_PROPERTY_NAME = "qualitygate.default";

  private final DbClient dbClient;

  public QualityGateFinder(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public QualityGateData getQualityGate(DbSession dbSession, ProjectDto projectDto) {
    return getQualityGate(dbSession, projectDto.getUuid());
  }

  public QualityGateData getQualityGate(DbSession dbSession, String projectUuid) {
    Optional<QualityGateData> res = getQualityGateForProject(dbSession, projectUuid);
    if (res.isPresent()) {
      return res.get();
    }
    QualityGateDto defaultQualityGate = getDefault(dbSession);
    return new QualityGateData(defaultQualityGate, true);
  }

  private Optional<QualityGateData> getQualityGateForProject(DbSession dbSession, String projectUuid) {
    return dbClient.projectQgateAssociationDao().selectQGateUuidByProjectUuid(dbSession, projectUuid)
      .map(qualityGateUuid -> dbClient.qualityGateDao().selectByUuid(dbSession, qualityGateUuid))
      .map(qualityGateDto -> new QualityGateData(qualityGateDto, false));
  }

  public QualityGateDto getDefault(DbSession dbSession) {
    PropertyDto qGateDefaultUuidProperty = dbClient.propertiesDao().selectGlobalProperty(dbSession, DEFAULT_QUALITY_GATE_PROPERTY_NAME);
    checkState(qGateDefaultUuidProperty != null, "Default quality gate property is missing");
    dbClient.qualityGateDao().selectByUuid(dbSession, qGateDefaultUuidProperty.getValue());
    return Optional.ofNullable(dbClient.qualityGateDao().selectByUuid(dbSession, qGateDefaultUuidProperty.getValue()))
      .orElseThrow(() -> new IllegalStateException("Default quality gate is missing"));
  }

  public QualityGateDto getBuiltInQualityGate(DbSession dbSession) {
    QualityGateDto builtIn = dbClient.qualityGateDao().selectBuiltIn(dbSession);
    checkState(builtIn != null, "Builtin quality gate is missing.");
    return builtIn;
  }

  public static class QualityGateData {
    private final QualityGateDto qualityGate;
    private final boolean isDefault;

    private QualityGateData(QualityGateDto qualityGate, boolean isDefault) {
      this.qualityGate = qualityGate;
      this.isDefault = isDefault;
    }

    public QualityGateDto getQualityGate() {
      return qualityGate;
    }

    public boolean isDefault() {
      return isDefault;
    }
  }

}
