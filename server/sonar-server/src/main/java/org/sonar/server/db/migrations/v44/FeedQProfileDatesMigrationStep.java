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

package org.sonar.server.db.migrations.v44;

import java.util.Date;

import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.UtcDateUtils;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.migration.v44.Migration44Mapper;
import org.sonar.core.persistence.migration.v44.QProfileDto44;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.migrations.MigrationStep;

/**
 * Feed the new columns RULES_PROFILES.CREATED_AT and UPDATED_AT
 * 
 * @since 4.4
 */
public class FeedQProfileDatesMigrationStep implements MigrationStep {

  private final DbClient db;
  private final System2 system;

  public FeedQProfileDatesMigrationStep(DbClient db, System2 system) {
    this.db = db;
    this.system = system;
  }

  @Override
  public void execute() {
    DbSession session = db.openSession(false);
    try {
      Date now = new Date(system.now());
      int i = 0;
      Migration44Mapper migrationMapper = session.getMapper(Migration44Mapper.class);
      for (QProfileDto44 profile : migrationMapper.selectAllProfiles()) {
        Date createdAt = (Date) ObjectUtils.defaultIfNull(migrationMapper.selectProfileCreatedAt(profile.getId()), now);
        Date updatedAt = (Date) ObjectUtils.defaultIfNull(migrationMapper.selectProfileUpdatedAt(profile.getId()), now);

        migrationMapper.updateProfileDates(profile.getId(), createdAt, updatedAt, UtcDateUtils.formatDateTime(updatedAt));
        if (i % 100 == 0) {
          session.commit();
          i++;
        }
      }
      session.commit();
    } finally {
      session.close();
    }
  }
}
