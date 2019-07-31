/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateDto;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Optional.ofNullable;

public class QualityGateFinder {

  private final DbClient dbClient;

  public QualityGateFinder(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Return effective quality gate of a project.
   *
   * It will first try to get the quality gate explicitly defined on a project, if none it will try to return default quality gate of the organization
   */
  public Optional<QualityGateData> getQualityGate(DbSession dbSession, OrganizationDto organization, ComponentDto component) {
    Optional<QualityGateData> res = dbClient.projectQgateAssociationDao().selectQGateUuidByComponentUuid(dbSession, component.uuid())
      .map(qualityGateUuid -> dbClient.qualityGateDao().selectByUuid(dbSession, qualityGateUuid))
      .map(qualityGateDto -> new QualityGateData(qualityGateDto, false));
    if (res.isPresent()) {
      return res;
    }
    return ofNullable(dbClient.qualityGateDao().selectByOrganizationAndUuid(dbSession, organization, organization.getDefaultQualityGateUuid()))
      .map(qualityGateDto -> new QualityGateData(qualityGateDto, true));
  }

  public QualityGateDto getDefault(DbSession dbSession, OrganizationDto organization) {
    QGateWithOrgDto qgate = dbClient.qualityGateDao().selectByOrganizationAndUuid(dbSession, organization, organization.getDefaultQualityGateUuid());
    checkState(qgate != null, "Default quality gate [%s] is missing on organization [%s]", organization.getDefaultQualityGateUuid(), organization.getUuid());
    return qgate;
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
