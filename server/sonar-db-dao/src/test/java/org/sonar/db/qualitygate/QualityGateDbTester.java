/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

public class QualityGateDbTester {

  private final DbTester db;
  private final DbClient dbClient;
  private final DbSession dbSession;

  public QualityGateDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  public QualityGateDto insertQualityGate() {
    return insertQualityGate(randomAlphanumeric(30));
  }

  public QualityGateDto insertQualityGate(String name) {
    QualityGateDto updatedUser = dbClient.qualityGateDao().insert(dbSession, new QualityGateDto().setName(name));
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
}
