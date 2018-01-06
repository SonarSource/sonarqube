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
package org.sonar.server.platform.db.migration.version.v63;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateOrganizationUuidToProjectsTest {

  private static final String ORGANIZATION_UUID = "some uuid";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(PopulateOrganizationUuidToProjectsTest.class, "projects_with_nullable_organization.sql");

  private PopulateOrganizationUuidToProjects underTest = new PopulateOrganizationUuidToProjects(dbTester.database(), new TestDefaultOrganizationUuidProvider(ORGANIZATION_UUID));

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
  public void execute_adds_organization_uuid_to_all_rows() throws SQLException {
    String root1 = insertRootComponent(1);
    insertComponent(11, root1);
    insertComponent(12, root1);
    String root2 = insertRootComponent(2);
    for (int i = 0; i < 100; i++) {
      insertComponent(100 + i, root2);
    }
    // no child root
    insertRootComponent(3);
    // non root row which root does not exist
    insertComponent(4, "non_existent_parent");
    int rowCount = dbTester.countRowsOfTable("projects");

    underTest.execute();

    String sql = "select count(*) from projects where organization_uuid is null";
    assertThat(countValueOf(sql)).isEqualTo(0);
    assertThat(countValueOf("select count(*) from projects where organization_uuid = '" + ORGANIZATION_UUID + "'")).isEqualTo(rowCount);
  }

  @Test
  public void execute_is_reentrant_when_table_had_data() throws SQLException {
    String root1 = insertRootComponent(1);
    insertComponent(11, root1);
    insertComponent(12, root1);
    insertRootComponent(2);

    underTest.execute();

    underTest.execute();
  }

  private Long countValueOf(String sql) {
    return (Long) dbTester.select(sql).get(0).entrySet().iterator().next().getValue();
  }

  private String insertRootComponent(int id) {
    String uuid = "uuid_" + id;
    dbTester.executeInsert(
      "projects",
      "ID", valueOf(id),
      "UUID", uuid,
      "ROOT_UUID", uuid,
      "PROJECT_UUID", uuid,
      "UUID_PATH", uuid + ".");
    return uuid;
  }

  private String insertComponent(int id, String project_uuid) {
    String uuid = "uuid_" + id;
    dbTester.executeInsert(
      "projects",
      "ID", valueOf(id),
      "UUID", uuid,
      "ROOT_UUID", project_uuid,
      "PROJECT_UUID", project_uuid,
      "UUID_PATH", uuid + "." + project_uuid);
    return uuid;
  }
}
