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
package org.sonar.db.version.v52;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.sql.SQLException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.MassUpdate.Handler;
import org.sonar.db.version.Select;
import org.sonar.db.version.Select.Row;
import org.sonar.db.version.Select.RowReader;
import org.sonar.db.version.SqlStatement;
import org.sonar.db.version.Upsert;

/**
 * SonarQube 5.2
 * SONAR-6328
 *
 */
public class MoveProjectProfileAssociation extends BaseDataChange {

  private static final class ProjectProfileAssociationHandler implements Handler {
    private final Upsert setDefaultProfile;
    private final Upsert associateProjectToProfile;
    private final Table<String, String, String> profileKeysByLanguageThenName;

    private ProjectProfileAssociationHandler(Upsert setDefaultProfile, Upsert associateProjectToProfile, Table<String, String, String> profileKeysByLanguageThenName) {
      this.setDefaultProfile = setDefaultProfile;
      this.associateProjectToProfile = associateProjectToProfile;
      this.profileKeysByLanguageThenName = profileKeysByLanguageThenName;
    }

    @Override
    public boolean handle(Row row, SqlStatement update) throws SQLException {
      Long id = row.getLong(1);
      String profileLanguage = extractLanguage(row.getString(2));
      String profileName = row.getString(3);
      Long projectId = row.getNullableLong(4);
      String projectUuid = row.getString(5);

      if (profileKeysByLanguageThenName.contains(profileLanguage, profileName)) {
        String profileKey = profileKeysByLanguageThenName.get(profileLanguage, profileName);

        if (projectUuid == null) {
          if (projectId == null) {
            setDefaultProfile.setBoolean(1, true).setString(2, profileKey).execute();
          } else {
            LOGGER.warn(String.format("Profile with language '%s' and name '%s' is associated with unknown project '%d', ignored", profileLanguage, profileName, projectId));
          }
        } else {
          associateProjectToProfile.setString(1, projectUuid).setString(2, profileKey).execute();
        }
      } else {
        LOGGER.warn(String.format("Unable to find profile with language '%s' and name '%s', ignored", profileLanguage, profileName));
      }

      update.setLong(1, id);
      return true;
    }
  }

  private static final Logger LOGGER = Loggers.get(MoveProjectProfileAssociation.class);

  public MoveProjectProfileAssociation(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {

    final Table<String, String, String> profileKeysByLanguageThenName = getProfileKeysByLanguageThenName(context);

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT prop.id, prop.prop_key, prop.text_value, prop.resource_id, proj.uuid " +
      "FROM properties prop " +
      "LEFT OUTER JOIN projects proj ON prop.resource_id = proj.id " +
      "WHERE prop.prop_key LIKE 'sonar.profile.%'"
      );
    massUpdate.update("DELETE FROM properties WHERE id = ?");

    final Upsert setDefaultProfile = context.prepareUpsert("UPDATE rules_profiles SET is_default = ? WHERE kee = ?");
    final Upsert associateProjectToProfile = context.prepareUpsert("INSERT INTO project_qprofiles (project_uuid, profile_key) VALUES (?, ?)");

    try {
      massUpdate.execute(new ProjectProfileAssociationHandler(setDefaultProfile, associateProjectToProfile, profileKeysByLanguageThenName));
    } finally {
      associateProjectToProfile.close();
      setDefaultProfile.close();
    }
  }

  private static String extractLanguage(String propertyKey) {
    return propertyKey.substring("sonar.profile.".length());
  }

  private Table<String, String, String> getProfileKeysByLanguageThenName(final Context context) throws SQLException {
    final Table<String, String, String> profilesByLanguageAndName = HashBasedTable.create();

    Select selectProfiles = context.prepareSelect("SELECT kee, name, language FROM rules_profiles");
    try {
      selectProfiles.list(new RowReader<Void>() {
        @Override
        public Void read(Row row) throws SQLException {
          profilesByLanguageAndName.put(row.getString(3), row.getString(2), row.getString(1));
          return null;
        }
      });
    } finally {
      selectProfiles.close();
    }

    return profilesByLanguageAndName;
  }
}
