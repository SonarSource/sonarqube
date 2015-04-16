/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.db.migrations.v45;

import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.migration.v44.Migration44Mapper;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.migrations.MigrationStep;
import org.sonar.server.db.migrations.v44.ConvertProfileMeasuresMigrationStep;

/**
 * See http://jira.codehaus.org/browse/SONAR-5580
 * {@link ConvertProfileMeasuresMigrationStep}
 * introduced a regression in 4.4. Measures on orphan profiles were kept 
 * in old format (no JSON data but numeric value of profile id)
 * 
 * @see ConvertProfileMeasuresMigrationStep
 * @since 4.5
 */
public class DeleteMeasuresOnDeletedProfilesMigrationStep implements MigrationStep {

  private final DbClient db;

  public DeleteMeasuresOnDeletedProfilesMigrationStep(DbClient db) {
    this.db = db;
  }

  @Override
  public void execute() {
    DbSession session = db.openSession(false);
    try {
      Migration44Mapper mapper = session.getMapper(Migration44Mapper.class);
      for (Long measureId : mapper.selectMeasuresOnDeletedQualityProfiles()) {
        mapper.deleteProfileMeasure(measureId);
      }
      session.commit();

    } finally {
      session.close();
    }
  }
}
