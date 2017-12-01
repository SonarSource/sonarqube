/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.function.Consumer;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.property.PropertyDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.RandomStringUtils.randomNumeric;

public class QualityGateDbTester {

  private final DbTester db;
  private final DbClient dbClient;
  private final DbSession dbSession;

  public QualityGateDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  public QualityGateDto insertQualityGate(String name) {
    return insertQualityGate(qualityGate -> qualityGate.setName(name));
  }

  @SafeVarargs
  public final QualityGateDto insertQualityGate(Consumer<QualityGateDto>... dtoPopulators) {
    QualityGateDto qualityGate = new QualityGateDto()
      .setName(randomAlphanumeric(30))
      .setUuid(Uuids.createFast())
      .setBuiltIn(false);
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(qualityGate));
    QualityGateDto updatedUser = dbClient.qualityGateDao().insert(dbSession, qualityGate);
    db.commit();
    return updatedUser;
  }

  public void associateProjectToQualityGate(ComponentDto component, QualityGateDto qualityGate) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
      .setKey("sonar.qualitygate")
      .setResourceId(component.getId())
      .setValue(String.valueOf(qualityGate.getId())));
    db.commit();
  }

  public QualityGateDto createDefaultQualityGate(String qualityGateName) {
    QualityGateDto defaultQGate = insertQualityGate(qualityGateName);
    setDefaultQualityGate(defaultQGate);
    return defaultQGate;
  }

  public void setDefaultQualityGate(QualityGateDto qualityGate) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
      .setKey("sonar.qualitygate")
      .setValue(String.valueOf(qualityGate.getId())));
    db.commit();
  }

  @SafeVarargs
  public final QualityGateConditionDto addCondition(QualityGateDto qualityGate, MetricDto metric, Consumer<QualityGateConditionDto>... dtoPopulators) {
    QualityGateConditionDto condition = new QualityGateConditionDto().setQualityGateId(qualityGate.getId())
      .setMetricId(metric.getId())
      .setOperator("GT")
      .setWarningThreshold(randomNumeric(10))
      .setErrorThreshold(randomNumeric(10));
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(condition));
    dbClient.gateConditionDao().insert(condition, dbSession);
    db.commit();
    return condition;
  }
}
