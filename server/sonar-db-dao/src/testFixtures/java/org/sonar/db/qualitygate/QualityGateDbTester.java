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
package org.sonar.db.qualitygate;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.RandomStringUtils.randomNumeric;

public class QualityGateDbTester {
  private static final String DEFAULT_QUALITY_GATE_PROPERTY_NAME = "qualitygate.default";

  private final DbTester db;
  private final DbClient dbClient;
  private final DbSession dbSession;

  public QualityGateDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  public QualityGateDto insertBuiltInQualityGate() {
    QualityGateDto builtin = dbClient.qualityGateDao().insert(dbSession, new QualityGateDto()
      .setName("Sonar way")
      .setUuid(Uuids.createFast())
      .setBuiltIn(true)
      .setCreatedAt(new Date()));
    dbSession.commit();
    return builtin;
  }

  @SafeVarargs
  public final QualityGateDto insertQualityGate(Consumer<QualityGateDto>... dtoPopulators) {
    QualityGateDto qualityGate = new QualityGateDto()
      .setName(randomAlphanumeric(30))
      .setUuid(Uuids.createFast())
      .setBuiltIn(false);
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(qualityGate));
    dbClient.qualityGateDao().insert(dbSession, qualityGate);
    db.commit();
    return dbClient.qualityGateDao().selectByUuid(dbSession, qualityGate.getUuid());
  }

  public void associateProjectToQualityGate(ProjectDto project, QualityGateDto qualityGate) {
    dbClient.projectQgateAssociationDao().insertProjectQGateAssociation(dbSession, project.getUuid(), qualityGate.getUuid());
    db.commit();
  }

  @SafeVarargs
  public final QualityGateDto createDefaultQualityGate(Consumer<QualityGateDto>... dtoPopulators) {
    QualityGateDto defaultQGate = insertQualityGate(dtoPopulators);
    setDefaultQualityGate(defaultQGate);
    return defaultQGate;
  }

  public void setDefaultQualityGate(QualityGateDto qualityGate) {
    dbClient.propertiesDao().saveProperty(new PropertyDto().setKey(DEFAULT_QUALITY_GATE_PROPERTY_NAME).setValue(qualityGate.getUuid()));
    dbSession.commit();
  }

  @SafeVarargs
  public final QualityGateConditionDto addCondition(QualityGateDto qualityGate, MetricDto metric, Consumer<QualityGateConditionDto>... dtoPopulators) {
    QualityGateConditionDto condition = new QualityGateConditionDto().setQualityGateUuid(qualityGate.getUuid())
      .setUuid(Uuids.createFast())
      .setMetricUuid(metric.getUuid())
      .setOperator("GT")
      .setErrorThreshold(randomNumeric(10));
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(condition));
    dbClient.gateConditionDao().insert(condition, dbSession);
    db.commit();
    return condition;
  }

  public Optional<String> selectQGateUuidByComponentUuid(String componentUuid) {
    return dbClient.projectQgateAssociationDao().selectQGateUuidByProjectUuid(dbSession, componentUuid);
  }

  public void addGroupPermission(QualityGateDto qualityGateDto, GroupDto group) {
    dbClient.qualityGateGroupPermissionsDao().insert(dbSession, new QualityGateGroupPermissionsDto()
        .setUuid(Uuids.createFast())
        .setGroupUuid(group.getUuid())
        .setQualityGateUuid(qualityGateDto.getUuid()),
      qualityGateDto.getName(),
      group.getName()
    );
    dbSession.commit();
  }

  public void addUserPermission(QualityGateDto qualityGateDto, UserDto user) {
    dbClient.qualityGateUserPermissionDao().insert(dbSession, new QualityGateUserPermissionsDto()
        .setUuid(Uuids.createFast())
        .setUserUuid(user.getUuid())
        .setQualityGateUuid(qualityGateDto.getUuid()),
      qualityGateDto.getName(),
      user.getLogin());
    dbSession.commit();
  }
}
