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
package org.sonar.server.platform.db.migration.version.v56;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PopulateInitialSchemaTest {

  private static final long NOW = 1_500L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private System2 system2 = mock(System2.class);

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateInitialSchemaTest.class, "v56.sql");

  private PopulateInitialSchema underTest = new PopulateInitialSchema(db.database(), system2);

  @Test
  public void migration_inserts_users_and_groups() throws SQLException {
    when(system2.now()).thenReturn(NOW);

    underTest.execute();

    verifyGroup("sonar-administrators", "System administrators");
    verifyGroup("sonar-users", "Any new users created will automatically join this group");
    verifyRolesOfAdminsGroup();
    verifyRolesOfUsersGroup();
    verifyRolesOfAnyone();
    verifyAdminUser();
    verifyMembershipOfAdminUser();
  }

  private void verifyRolesOfAdminsGroup() {
    List<String> roles = selectRoles("sonar-administrators");
    assertThat(roles).containsOnly("admin", "profileadmin", "gateadmin", "shareDashboard", "provisioning");
  }

  private void verifyRolesOfUsersGroup() {
    assertThat(selectRoles("sonar-users")).isEmpty();
  }

  private void verifyRolesOfAnyone() {
    List<Map<String, Object>> rows = db.select("select gr.role as \"role\" " +
      "from group_roles gr where gr.group_id is null");
    List<String> roles = rows.stream()
      .map(row -> (String) row.get("role"))
      .collect(MoreCollectors.toArrayList());
    assertThat(roles).containsOnly("provisioning", "scan");
  }

  private List<String> selectRoles(String groupName) {
    List<Map<String, Object>> rows = db.select("select gr.role as \"role\" " +
      "from group_roles gr " +
      "inner join groups g on gr.group_id = g.id " +
      "where g.name='" + groupName + "'");
    return rows.stream()
      .map(row -> (String) row.get("role"))
      .collect(MoreCollectors.toArrayList());
  }

  private void verifyGroup(String expectedName, String expectedDescription) {
    Map<String, Object> cols = db.selectFirst("select name as \"name\", description as \"description\", " +
      "created_at as \"created_at\", updated_at as \"updated_at\" " +
      "from groups where name='" + expectedName + "'");
    assertThat(cols.get("name")).isEqualTo(expectedName);
    assertThat(cols.get("description")).isEqualTo(expectedDescription);
    assertThat(cols.get("created_at")).isNotNull();
  }

  private void verifyMembershipOfAdminUser() {
    List<Map<String, Object>> rows = db.select("select g.name as \"groupName\" from groups g " +
      "inner join groups_users gu on gu.group_id = g.id " +
      "inner join users u on gu.user_id = u.id " +
      "where u.login='admin'");
    List<String> groupNames = rows.stream()
      .map(row -> (String) row.get("groupName"))
      .collect(MoreCollectors.toArrayList());
    assertThat(groupNames).containsOnly("sonar-administrators", "sonar-users");
  }

  private void verifyAdminUser() {
    Map<String, Object> cols = db.selectFirst("select login as \"login\", name as \"name\", email as \"email\", " +
      "external_identity as \"external_identity\", external_identity_provider as \"external_identity_provider\", " +
      "user_local as \"user_local\", crypted_password as \"crypted_password\", salt as \"salt\", created_at as \"created_at\", " +
      "updated_at as \"updated_at\", remember_token as \"remember_token\", remember_token_expires_at as \"remember_token_expires_at\" " +
      "from users where login='admin'");
    assertThat(cols.get("login")).isEqualTo("admin");
    assertThat(cols.get("name")).isEqualTo("Administrator");
    assertThat(cols.get("email")).isEqualTo("");
    assertThat(cols.get("user_local")).isEqualTo(true);
    assertThat(cols.get("crypted_password")).isEqualTo("a373a0e667abb2604c1fd571eb4ad47fe8cc0878");
    assertThat(cols.get("salt")).isEqualTo("48bc4b0d93179b5103fd3885ea9119498e9d161b");
    assertThat(cols.get("created_at")).isEqualTo(NOW);
    assertThat(cols.get("updated_at")).isEqualTo(NOW);
    assertThat(cols.get("remember_token")).isNull();
    assertThat(cols.get("remember_token_expires_at")).isNull();
  }

}
