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

import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.util.Validation.IS_ALREADY_USED_MESSAGE;

public class QualityGateUpdater {

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;

  public QualityGateUpdater(DbClient dbClient, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
  }

  public QualityGateDto create(DbSession dbSession, OrganizationDto organizationDto, String name) {
    validateQualityGate(dbSession, organizationDto, name);
    QualityGateDto newQualityGate = new QualityGateDto()
      .setName(name)
      .setBuiltIn(false)
      .setUuid(uuidFactory.create());
    dbClient.qualityGateDao().insert(dbSession, newQualityGate);
    dbClient.qualityGateDao().associate(dbSession, uuidFactory.create(), organizationDto, newQualityGate);
    return newQualityGate;
  }

  public QualityGateDto copy(DbSession dbSession, OrganizationDto organizationDto, QualityGateDto qualityGateDto, String destinationName) {
    QualityGateDto destinationGate = create(dbSession, organizationDto, destinationName);
    for (QualityGateConditionDto sourceCondition : dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGateDto.getUuid())) {
      dbClient.gateConditionDao().insert(new QualityGateConditionDto()
        .setUuid(Uuids.create())
        .setQualityGateUuid(destinationGate.getUuid())
        .setMetricUuid(sourceCondition.getMetricUuid())
        .setOperator(sourceCondition.getOperator())
        .setErrorThreshold(sourceCondition.getErrorThreshold()),
        dbSession);
    }

    return destinationGate;
  }

  private void validateQualityGate(DbSession dbSession, OrganizationDto organizationDto, String name) {
    checkQualityGateDoesNotAlreadyExist(dbSession, organizationDto, name);
  }

  private void checkQualityGateDoesNotAlreadyExist(DbSession dbSession, OrganizationDto organizationDto, String name) {
    QualityGateDto existingQGate = dbClient.qualityGateDao().selectByOrganizationAndName(dbSession, organizationDto, name);
    checkArgument(existingQGate == null, IS_ALREADY_USED_MESSAGE, "Name");
  }
}
