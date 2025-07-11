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
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static org.apache.commons.lang3.RandomStringUtils.secure;


public class QualityGateDbTester {
  private static final String DEFAULT_QUALITY_GATE_PROPERTY_NAME = "qualitygate.default";

  private final DbTester db;
  private final DbClient dbClient;

  public QualityGateDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
  }

  public QualityGateDto insertBuiltInQualityGate() {
    QualityGateDto builtin = dbClient.qualityGateDao().insert(db.getSession(), new QualityGateDto()
      .setName("Sonar way")
      .setUuid(Uuids.createFast())
      .setBuiltIn(true)
      .setCreatedAt(new Date()));
    db.commit();
    return builtin;
  }

  @SafeVarargs
  public final QualityGateDto insertQualityGate(Consumer<QualityGateDto>... dtoPopulators) {
    QualityGateDto qualityGate = new QualityGateDto()
      .setName(secure().nextAlphanumeric(30))
      .setUuid(Uuids.createFast())
      .setBuiltIn(false);
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(qualityGate));
    dbClient.qualityGateDao().insert(db.getSession(), qualityGate);
    db.commit();
    return dbClient.qualityGateDao().selectByUuid(db.getSession(), qualityGate.getUuid());
  }

  public void associateProjectToQualityGate(ProjectDto project, QualityGateDto qualityGate) {
    dbClient.projectQgateAssociationDao().insertProjectQGateAssociation(db.getSession(), project.getUuid(), qualityGate.getUuid());
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
    db.commit();
  }

  @SafeVarargs
  public final QualityGateConditionDto addCondition(QualityGateDto qualityGate, MetricDto metric, Consumer<QualityGateConditionDto>... dtoPopulators) {
    QualityGateConditionDto condition = new QualityGateConditionDto().setQualityGateUuid(qualityGate.getUuid())
      .setUuid(Uuids.createFast())
      .setMetricUuid(metric.getUuid())
      .setOperator("GT")
      .setErrorThreshold(secure().nextNumeric(10));
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(condition));
    dbClient.gateConditionDao().insert(condition, db.getSession());
    db.commit();
    return condition;
  }

  public Optional<String> selectQGateUuidByProjectUuid(String projectUuid) {
    return dbClient.projectQgateAssociationDao().selectQGateUuidByProjectUuid(db.getSession(), projectUuid);
  }

  public void addGroupPermission(QualityGateDto qualityGateDto, GroupDto group) {
    dbClient.qualityGateGroupPermissionsDao().insert(db.getSession(), new QualityGateGroupPermissionsDto()
        .setUuid(Uuids.createFast())
        .setGroupUuid(group.getUuid())
        .setQualityGateUuid(qualityGateDto.getUuid()),
      qualityGateDto.getName(),
      group.getName(), qualityGateDto.getOrganizationUuid()
    );
    db.commit();
  }

  public void addUserPermission(QualityGateDto qualityGateDto, UserDto user) {
    dbClient.qualityGateUserPermissionDao().insert(db.getSession(), new QualityGateUserPermissionsDto()
        .setUuid(Uuids.createFast())
        .setUserUuid(user.getUuid())
        .setQualityGateUuid(qualityGateDto.getUuid()),
      qualityGateDto.getName(),
      user.getLogin(), qualityGateDto.getOrganizationUuid());
    db.commit();
  }
}
