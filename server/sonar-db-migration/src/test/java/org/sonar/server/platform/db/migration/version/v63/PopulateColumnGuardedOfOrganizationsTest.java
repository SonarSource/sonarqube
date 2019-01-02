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
package org.sonar.server.platform.db.migration.version.v63;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateColumnGuardedOfOrganizationsTest {

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(PopulateColumnGuardedOfOrganizationsTest.class, "organizations.sql");

  private PopulateColumnGuardedOfOrganizations underTest = new PopulateColumnGuardedOfOrganizations(dbTester.database());

  @Test
  public void execute_has_no_effect_when_table_is_empty() throws SQLException {
    underTest.execute();
  }

  @Test
  public void execute_is_reentrant_when_table_is_empty() throws SQLException {
    underTest.execute();

    underTest.execute();
  }

  @Test
  public void execute_sets_is_protected_to_false_for_all_rows() throws SQLException {
    insertOrganization("u1", null);
    insertOrganization("u2", null);

    underTest.execute();

    assertThat(dbTester.countSql("select count(*) from organizations where guarded is null")).isEqualTo(0);
    assertThat(dbTester.countSql("select count(*) from organizations where guarded is not null")).isEqualTo(2);
    assertThat(dbTester.countSql("select count(*) from organizations where guarded=false")).isEqualTo(2);
  }

  @Test
  public void execute_is_reentrant_when_table_had_data() throws SQLException {
    insertOrganization("u1", true);
    insertOrganization("u2", null);

    underTest.execute();

    assertThat(dbTester.countSql("select count(*) from organizations where guarded is null")).isEqualTo(0);
    assertThat(dbTester.countSql("select count(*) from organizations where guarded is not null")).isEqualTo(2);
    assertThat(dbTester.countSql("select count(*) from organizations where guarded=false")).isEqualTo(1);
    assertThat(dbTester.countSql("select count(*) from organizations where guarded=true")).isEqualTo(1);

    underTest.execute();
  }

  @Test
  public void execute_is_reentrant_when_table_had_partially_migrated_data() throws SQLException {
    insertOrganization("u1", false);
    insertOrganization("u2", null);

    underTest.execute();
  }

  private void insertOrganization(String uuid, @Nullable Boolean guarded) {
    dbTester.executeInsert(
      "ORGANIZATIONS",
      "UUID", uuid,
      "KEE", uuid,
      "NAME", uuid,
      "GUARDED", guarded == null ? null : String.valueOf(guarded),
      "CREATED_AT", "1000",
      "UPDATED_AT", "1000");
  }

}
