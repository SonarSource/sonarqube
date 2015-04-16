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

import java.io.StringWriter;
import java.util.Date;

import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.UtcDateUtils;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.migration.v44.Migration44Mapper;
import org.sonar.core.persistence.migration.v44.ProfileMeasure;
import org.sonar.core.persistence.migration.v44.QProfileDto44;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.migrations.MigrationStep;

/**
 * Feed the new columns RULES_PROFILE.KEE and PARENT_KEE.
 * 
 * @since 4.4
 */
public class ConvertProfileMeasuresMigrationStep implements MigrationStep {

  private final DbClient db;

  public ConvertProfileMeasuresMigrationStep(DbClient db) {
    this.db = db;
  }

  @Override
  public void execute() {
    DbSession session = db.openSession(false);
    try {
      int i = 0;
      Date now = new Date();
      Migration44Mapper mapper = session.getMapper(Migration44Mapper.class);
      for (ProfileMeasure profileMeasure : mapper.selectProfileMeasures()) {
        boolean updated = false;
        Integer version = mapper.selectProfileVersion(profileMeasure.getSnapshotId());
        QProfileDto44 profile = mapper.selectProfileById(profileMeasure.getProfileId());
        if (profile != null) {
          Date date = now;
          if (version != null) {
            date = (Date) ObjectUtils.defaultIfNull(
              mapper.selectProfileVersionDate(profileMeasure.getProfileId(), version), now);
          }
          // see format of JSON in org.sonar.batch.rule.UsedQProfiles
          StringWriter writer = new StringWriter();
          JsonWriter json = JsonWriter.of(writer);
          json
            .beginArray()
            .beginObject()
            .prop("key", profile.getKee())
            .prop("language", profile.getLanguage())
            .prop("name", profile.getName())
            .prop("rulesUpdatedAt", UtcDateUtils.formatDateTime(date))
            .endObject()
            .endArray()
            .close();
          mapper.updateProfileMeasure(profileMeasure.getId(), writer.toString());
          updated = true;
        }
        if (!updated) {
          mapper.deleteProfileMeasure(profileMeasure.getId());
        }
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
