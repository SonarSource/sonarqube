/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import java.util.Date;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProvider;

import static java.util.Optional.ofNullable;

public class PopulateRulesMetadata extends DataChange {
  private final DefaultOrganizationUuidProvider defaultOrganizationUuid;
  private final System2 system2;

  public PopulateRulesMetadata(Database db, DefaultOrganizationUuidProvider defaultOrganizationUuid, System2 system2) {
    super(db);
    this.defaultOrganizationUuid = defaultOrganizationUuid;
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String defaultOrganizationUuid = this.defaultOrganizationUuid.getAndCheck(context);

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      " id," +
      " note_data," +
      " note_user_login," +
      " note_created_at," +
      " note_updated_at," +
      " remediation_function," +
      " remediation_gap_mult," +
      " remediation_base_effort," +
      " tags," +
      " created_at," +
      " updated_at" +
      " from" +
      "  rules r" +
      " where" +
      "  not exists (select 1 from rules_metadata rm where rm.rule_id = r.id)");
    massUpdate.rowPluralName("rules metadata");
    massUpdate.update("insert into rules_metadata" +
      " (" +
      " rule_id," +
      " organization_uuid," +
      " note_data," +
      " note_user_login," +
      " note_created_at," +
      " note_updated_at," +
      " remediation_function," +
      " remediation_gap_mult," +
      " remediation_base_effort," +
      " tags," +
      " created_at," +
      " updated_at" +
      ")" +
      "values" +
      "(" +
      " ?," +
      " ?," +
      " ?," +
      " ?," +
      " ?," +
      " ?," +
      " ?," +
      " ?," +
      " ?," +
      " ?," +
      " ?," +
      " ?" +
      ")");
    massUpdate.execute((row, update) -> handle(defaultOrganizationUuid, row, update));
  }

  private boolean handle(String defaultOrganizationUuid, Select.Row row, SqlStatement update) throws SQLException {
    long now = system2.now();
    int ruleId = row.getInt(1);
    String noteData = row.getNullableString(2);
    String noteUserLogin = row.getNullableString(3);
    Date noteCreatedAt = row.getNullableDate(4);
    Date noteUpdatedAt = row.getNullableDate(5);
    String remediationFunction = row.getNullableString(6);
    String remediationGapMultiplier = row.getNullableString(7);
    String remediationBaseEffort = row.getNullableString(8);
    String tags = row.getNullableString(9);
    Long createdAt = row.getNullableLong(10);
    Long updatedAt = row.getNullableLong(11);

    update
      .setInt(1, ruleId)
      .setString(2, defaultOrganizationUuid)
      .setString(3, noteData)
      .setString(4, noteUserLogin)
      .setLong(5, ofNullable(noteCreatedAt).map(Date::getTime).orElse(null))
      .setLong(6, ofNullable(noteUpdatedAt).map(Date::getTime).orElse(null))
      .setString(7, remediationFunction)
      .setString(8, remediationGapMultiplier)
      .setString(9, remediationBaseEffort)
      .setString(10, tags)
      .setLong(11, ofNullable(createdAt).orElse(now))
      .setLong(12, ofNullable(updatedAt).orElse(now));
    return true;
  }
}
