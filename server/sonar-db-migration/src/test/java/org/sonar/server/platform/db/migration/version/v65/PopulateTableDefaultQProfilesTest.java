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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class PopulateTableDefaultQProfilesTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateTableDefaultQProfilesTest.class, "initial.sql");

  private System2 system2 = new AlwaysIncreasingSystem2();
  private PopulateTableDefaultQProfiles underTest = new PopulateTableDefaultQProfiles(db.database(), system2);

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertRulesProfile("ORG_1", "java", "u1", true);
    insertRulesProfile("ORG_2", "js", "u2", true);

    // org1 is already processed
    insertDefaultQProfile("ORG_1", "java", "u1");

    underTest.execute();

    assertThat(countRows()).isEqualTo(2);
    assertThat(selectDefaultProfile("ORG_1", "java")).isEqualTo("u1");
    assertThat(selectDefaultProfile("ORG_2", "js")).isEqualTo("u2");
  }

  @Test
  public void DEFAULT_QPROFILES_is_populated_by_copying_the_RULES_PROFILES_marked_as_default() throws SQLException {
    insertRulesProfile("ORG_1", "java", "u1", false);
    insertRulesProfile("ORG_1", "java", "u2", true);
    insertRulesProfile("ORG_1", "js", "u3", true);

    underTest.execute();

    assertThat(countRows()).isEqualTo(2);
    assertThat(selectDefaultProfile("ORG_1", "java")).isEqualTo("u2");
    assertThat(selectDefaultProfile("ORG_1", "js")).isEqualTo("u3");
  }

  @Test
  public void duplicated_rows_of_table_RULES_PROFILES_are_ignored() throws SQLException {
    // two java profiles are marked as default.
    // The second one (as ordered by id) is ignored.
    insertRulesProfile("ORG_1", "java", "u1", false);
    insertRulesProfile("ORG_1", "java", "u2", true);
    insertRulesProfile("ORG_1", "java", "u3", true);

    underTest.execute();

    assertThat(countRows()).isEqualTo(1);
    assertThat(selectDefaultProfile("ORG_1", "java")).isEqualTo("u2");
  }

  private int countRows() {
    return db.countRowsOfTable("default_qprofiles");
  }

  private void insertRulesProfile(String orgUuid, String language, String uuid, boolean isDefault) {
    db.executeInsert("RULES_PROFILES",
      "NAME", "name_" + uuid,
      "KEE", uuid,
      "ORGANIZATION_UUID", orgUuid,
      "LANGUAGE", language,
      "IS_DEFAULT", isDefault,
      "IS_BUILT_IN", true);
  }

  private void insertDefaultQProfile(String orgUuid, String language, String uuid) {
    db.executeInsert("DEFAULT_QPROFILES",
      "ORGANIZATION_UUID", orgUuid,
      "LANGUAGE", language,
      "QPROFILE_UUID", uuid);
  }

  private String selectDefaultProfile(String orgUuid, String language) {
    return (String)db.selectFirst("select qprofile_uuid as QPROFILE_UUID from default_qprofiles where organization_uuid='" + orgUuid + "' and language='" + language + "'")
      .get("QPROFILE_UUID");
  }

}
