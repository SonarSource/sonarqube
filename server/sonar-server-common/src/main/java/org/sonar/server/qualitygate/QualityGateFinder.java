/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateDto;

public class QualityGateFinder {
  private final DbClient dbClient;

  public QualityGateFinder(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public QualityGateData getEffectiveQualityGate(DbSession dbSession, OrganizationDto organization, ProjectDto projectDto) {
    return getEffectiveQualityGate(dbSession, organization, projectDto.getUuid());
  }

  public QualityGateData getEffectiveQualityGate(DbSession dbSession, OrganizationDto organization, String projectUuid) {
    Optional<QualityGateData> res = getQualityGateForProject(dbSession, projectUuid);
    if (res.isPresent()) {
      return res.get();
    }
    QualityGateDto defaultQualityGate = getDefault(dbSession, organization);
    return new QualityGateData(defaultQualityGate, true);
  }

  private Optional<QualityGateData> getQualityGateForProject(DbSession dbSession, String projectUuid) {
    return dbClient.projectQgateAssociationDao().selectQGateUuidByProjectUuid(dbSession, projectUuid)
      .map(qualityGateUuid -> dbClient.qualityGateDao().selectByUuid(dbSession, qualityGateUuid))
      .map(qualityGateDto -> new QualityGateData(qualityGateDto, false));
  }

  public QualityGateDto getDefault(DbSession dbSession, OrganizationDto organization) {
    QualityGateDto qgate = dbClient.qualityGateDao().selectByOrganizationAndUuid(dbSession, organization, organization.getDefaultQualityGateUuid());
    checkState(qgate != null, "Default quality gate [%s] is missing on organization [%s]", organization.getDefaultQualityGateUuid(), organization.getUuid());
    return qgate;
  }

  public static class QualityGateData {
    private final String uuid;
    private final String name;
    private final boolean isDefault;
    private final boolean builtIn;

    private QualityGateData(QualityGateDto qualityGate, boolean isDefault) {
      this.uuid = qualityGate.getUuid();
      this.name = qualityGate.getName();
      this.isDefault = isDefault;
      this.builtIn = qualityGate.isBuiltIn();
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
  }

}
