/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v86;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateChangeDataOfQProfileChangesTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(UpdateChangeDataOfQProfileChangesTest.class, "schema.sql");

  private DataChange underTest = new UpdateChangeDataOfQProfileChanges(db.database());

  @Test
  public void change_ruleId_to_ruleUuid() throws SQLException {
    insertQProfileChanges("key-1", "severity=MAJOR;ruleUuid=AXJVoXWDOFGkZYIS4z4D");
    insertQProfileChanges("key-2", "severity=MAJOR;ruleId=12915");
    insertQProfileChanges("key-3", "severity=MAJOR;ruleId=12915;something");
    insertQProfileChanges("key-4", "ruleId=12915;something");

    underTest.execute();

    assertThatQProfileMigrated("key-1", "severity=MAJOR;ruleUuid=AXJVoXWDOFGkZYIS4z4D");
    assertThatQProfileMigrated("key-2", "severity=MAJOR;ruleUuid=12915");
    assertThatQProfileMigrated("key-3", "severity=MAJOR;ruleUuid=12915;something");
    assertThatQProfileMigrated("key-4", "ruleUuid=12915;something");
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertQProfileChanges("key-1", "severity=MAJOR;ruleUuid=AXJVoXWDOFGkZYIS4z4D");
    insertQProfileChanges("key-2", "severity=MAJOR;ruleId=12915");
    insertQProfileChanges("key-3", "severity=MAJOR;ruleId=12915;something");
    insertQProfileChanges("key-4", "ruleId=12915;something");

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThatQProfileMigrated("key-1", "severity=MAJOR;ruleUuid=AXJVoXWDOFGkZYIS4z4D");
    assertThatQProfileMigrated("key-2", "severity=MAJOR;ruleUuid=12915");
    assertThatQProfileMigrated("key-3", "severity=MAJOR;ruleUuid=12915;something");
    assertThatQProfileMigrated("key-4", "ruleUuid=12915;something");
  }

  private void assertThatQProfileMigrated(String key, String changeData) {
    assertThat(db.select("select qpc.kee, qpc.change_data "
      + "from qprofile_changes qpc where qpc.kee = '" + key + "'").stream().findFirst())
        .isNotEmpty()
        .hasValueSatisfying(stringObjectMap -> assertThat(stringObjectMap)
          .extractingByKeys("KEE", "CHANGE_DATA")
          .contains(key, changeData));

  }

  private void insertQProfileChanges(String kee, String changeData) {
    db.executeInsert("qprofile_changes",
      "kee", kee,
      "rules_profile_uuid", Uuids.createFast(),
      "change_type", "any",
      "change_data", changeData,
      "created_at", System.currentTimeMillis());
  }
}
