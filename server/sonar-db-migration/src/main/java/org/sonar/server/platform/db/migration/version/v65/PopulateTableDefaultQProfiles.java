/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;

/**
 * Move the list of profiles marked as "default" from rules_profiles.is_default
 * to the table default_qprofiles.
 */
public class PopulateTableDefaultQProfiles extends DataChange {

  private final System2 system2;

  public PopulateTableDefaultQProfiles(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Set<OrgLang> buggyOrgs = selectOrgsWithMultipleDefaultProfiles(context);
    Set<OrgLang> processedBuggyOrgs = new HashSet<>();
    long now = system2.now();

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select p.organization_uuid, p.language, p.kee from rules_profiles p " +
      " where p.is_default = ? " +
      " and not exists (select 1 from default_qprofiles dp where dp.organization_uuid = p.organization_uuid and dp.language = p.language)" +
      " order by id")
      .setBoolean(1, true);
    massUpdate.update("insert into default_qprofiles" +
      " (organization_uuid, language, qprofile_uuid, created_at, updated_at) values (?, ?, ?, ?, ?)");
    massUpdate.rowPluralName("default_qprofiles");
    massUpdate.execute((row, update) -> {
      OrgLang pk = new OrgLang(row.getString(1), row.getString(2));
      String profileUuid = row.getString(3);

      boolean isBuggy = buggyOrgs.contains(pk);
      if (isBuggy && processedBuggyOrgs.contains(pk)) {
        // profile is ignored. There's already one marked as default.
        return false;
      }
      update.setString(1, pk.orgUuid);
      update.setString(2, pk.language);
      update.setString(3, profileUuid);
      update.setLong(4, now);
      update.setLong(5, now);
      if (isBuggy) {
        processedBuggyOrgs.add(pk);
      }
      return true;
    });
  }

  /**
   * By design the table rules_profiles does not enforce to have a single
   * profile marked as default for an organization and language.
   * This method returns the buggy rows.
   */
  private static Set<OrgLang> selectOrgsWithMultipleDefaultProfiles(Context context) throws SQLException {
    Select rows = context.prepareSelect("select organization_uuid, language from rules_profiles " +
      " where is_default = ? " +
      " group by organization_uuid, language " +
      " having count(kee) > 1 ")
      .setBoolean(1, true);
    return new HashSet<>(rows.list(row -> new OrgLang(row.getString(1), row.getString(2))));
  }

  private static class OrgLang {
    private final String orgUuid;
    private final String language;

    private OrgLang(String orgUuid, String language) {
      this.orgUuid = orgUuid;
      this.language = language;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      OrgLang orgLang = (OrgLang) o;
      if (!orgUuid.equals(orgLang.orgUuid)) {
        return false;
      }
      return language.equals(orgLang.language);
    }

    @Override
    public int hashCode() {
      int result = orgUuid.hashCode();
      result = 31 * result + language.hashCode();
      return result;
    }
  }
}
