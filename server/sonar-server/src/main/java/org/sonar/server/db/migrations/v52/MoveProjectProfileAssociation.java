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
package org.sonar.server.db.migrations.v52;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.util.ProgressLogger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SonarQube 5.2
 * SONAR-6328
 *
 */
public class MoveProjectProfileAssociation implements DatabaseMigration {

  private static final Logger LOGGER = Loggers.get(MoveProjectProfileAssociation.class);

  private final DbClient db;
  private final AtomicLong counter = new AtomicLong(0L);

  public MoveProjectProfileAssociation(DbClient db) {
    this.db = db;

  }

  @Override
  public void execute() throws SQLException {
    ProgressLogger progress = ProgressLogger.create(getClass(), counter);
    progress.start();

    final DbSession readSession = db.openSession(false);
    final DbSession writeSession = db.openSession(true);

    PreparedStatement selectStatement = readSession.getConnection().prepareStatement(
      "SELECT prop.id, prop.prop_key, prop.text_value, prop.resource_id, proj.uuid " +
        "FROM properties prop " +
        "LEFT OUTER JOIN projects proj ON prop.resource_id = proj.id " +
        "WHERE prop.prop_key LIKE 'sonar.profile.%'"
      );
    PreparedStatement deleteProperty = writeSession.getConnection().prepareStatement(
      "DELETE FROM properties WHERE id = ?");

    try {
      Table<String, String, String> profileKeysByLanguageThenName = getProfileKeysByLanguageThenName(readSession);

      ResultSet profileProperties = selectStatement.executeQuery();

      Long id;
      String profileLanguage;
      String profileName;
      Long projectId;
      String projectUuid;

      while (profileProperties.next()) {
        id = profileProperties.getLong(1);
        profileLanguage = extractLanguage(profileProperties.getString(2));
        profileName = profileProperties.getString(3);
        projectId = profileProperties.getLong(4);
        projectUuid = profileProperties.getString(5);

        if (profileKeysByLanguageThenName.contains(profileLanguage, profileName)) {
          String profileKey = profileKeysByLanguageThenName.get(profileLanguage, profileName);

          if (projectUuid == null) {
            if (projectId == null || projectId == 0L) {
              setProfileIsDefault(profileKey, writeSession);
            } else {
              LOGGER.warn(String.format("Profile with language '%s' and name '%s' is associated with unknown project '%d', ignored", profileLanguage, profileName, projectId));
            }
          } else {
            associateProjectWithProfile(projectUuid, profileKey, writeSession);
          }
        } else {
          LOGGER.warn(String.format("Unable to find profile with language '%s' and name '%s', ignored", profileLanguage, profileName));
        }

        deleteProperty.setLong(1, id);
        deleteProperty.execute();

        counter.getAndIncrement();
      }

      writeSession.commit(true);
      readSession.commit(true);

      // log the total number of process rows
      progress.log();
    } finally {
      deleteProperty.close();
      selectStatement.close();
      readSession.close();
      writeSession.close();
      progress.stop();
    }

  }

  private String extractLanguage(String propertyKey) {
    return propertyKey.substring("sonar.profile.".length());
  }

  private void setProfileIsDefault(String profileKey, final DbSession writeSession) throws SQLException {
    PreparedStatement updateStatement = writeSession.getConnection().prepareStatement(
      "UPDATE rules_profiles SET is_default = ? WHERE kee = ?");
    try {
      PreparedStatement setDefaultProfile = updateStatement;
      setDefaultProfile.setBoolean(1, true);
      setDefaultProfile.setString(2, profileKey);
      setDefaultProfile.execute();
    } finally {
      updateStatement.close();
    }
  }

  private void associateProjectWithProfile(String projectUuid, String profileKey, final DbSession writeSession) throws SQLException {
    PreparedStatement insertStatement = writeSession.getConnection().prepareStatement(
      "INSERT INTO project_profiles (project_uuid, profile_key) VALUES (?, ?)");
    try {
      PreparedStatement insertAssociation = insertStatement;
      insertAssociation.setString(1, projectUuid);
      insertAssociation.setString(2, profileKey);
      insertAssociation.execute();
    } finally {
      insertStatement.close();
    }
  }

  private Table<String, String, String> getProfileKeysByLanguageThenName(final DbSession readSession) throws SQLException {
    Table<String, String, String> profilesByLanguageAndName = HashBasedTable.create();

    PreparedStatement selectStatement = readSession.getConnection().prepareStatement(
      "SELECT kee, name, language FROM rules_profiles"
      );
    try {
      ResultSet profiles = selectStatement.executeQuery();

      String key;
      String name;
      String language;
      while (profiles.next()) {
        key = profiles.getString(1);
        name = profiles.getString(2);
        language = profiles.getString(3);
        profilesByLanguageAndName.put(language, name, key);
      }
    } finally {
      selectStatement.close();
    }
    return profilesByLanguageAndName;
  }
}
