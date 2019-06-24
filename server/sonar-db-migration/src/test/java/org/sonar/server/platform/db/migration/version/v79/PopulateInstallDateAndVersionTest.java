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
package org.sonar.server.platform.db.migration.version.v79;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PopulateInstallDateAndVersionTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateInstallDateAndVersionTest.class, "schema.sql");

  private System2 system2 = mock(System2.class);
  private SonarRuntime sonarRuntime = mock(SonarRuntime.class);

  private DataChange underTest = new PopulateInstallDateAndVersion(db.database(), sonarRuntime, system2);

  private Function<Map<String, Object>, Object> field(String name) {
    return m -> m.get(name);
  }

  @Before
  public void before() {
    Version version = Version.create(7, 9, 0);
    when(sonarRuntime.getApiVersion()).thenReturn(version);
    when(system2.now()).thenReturn(RandomUtils.nextLong());
    truncateUsers();
    truncateInternalProperties();
  }

  @Test
  public void migrateFreshInstall() throws SQLException {
    Long createdAt = system2.now() - (23 * 60 * 60 * 1000);
    insertAdminUser(createdAt);

    underTest.execute();

    assertThat(db.select("select * from internal_properties")).extracting(
      field("CREATED_AT"), field("CLOB_VALUE"),
      field("KEE"), field("TEXT_VALUE"), field("IS_EMPTY"))
      .containsExactlyInAnyOrder(
        tuple(system2.now(), null, "installation.date", String.valueOf(createdAt), false),
        tuple(system2.now(), null, "installation.version", "7.9", false));
  }

  @Test
  public void migrateOldInstance() throws SQLException {
    Long createdAt = system2.now() - (25 * 60 * 60 * 1000);
    insertAdminUser(createdAt);

    underTest.execute();

    assertThat(db.select("select * from internal_properties")).extracting(
      field("CREATED_AT"), field("CLOB_VALUE"),
      field("KEE"), field("TEXT_VALUE"), field("IS_EMPTY"))
      .containsExactlyInAnyOrder(
        tuple(system2.now(), null, "installation.date", String.valueOf(createdAt), false));
  }

  @Test
  public void migrateNoUsers() throws SQLException {
    underTest.execute();

    assertThat(db.select("select * from internal_properties").stream().count()).isEqualTo(0);
  }

  private void insertAdminUser(long createdAt) {
    Map<String, Object> values = new HashMap<>();
    values.put("UUID", "UUID");
    values.put("login", "admin");
    values.put("name", "Administrator");
    values.put("email", null);
    values.put("EXTERNAL_ID", "admin");
    values.put("EXTERNAL_LOGIN", "admin");
    values.put("external_identity_provider", "sonarqube");
    values.put("user_local", true);
    values.put("crypted_password", "a373a0e667abb2604c1fd571eb4ad47fe8cc0878");
    values.put("salt", "48bc4b0d93179b5103fd3885ea9119498e9d161b");
    values.put("created_at", createdAt);
    values.put("updated_at", createdAt);
    values.put("IS_ROOT", true);
    values.put("ONBOARDED", false);
    db.executeInsert("users", values);
  }

  private void truncateUsers() {
    db.executeUpdateSql("truncate table users");
  }

  private void truncateInternalProperties() {
    db.executeUpdateSql("truncate table internal_properties");
  }
}
