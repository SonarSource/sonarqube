/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtemplatesusers;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulatePermTemplatesUsersTemplateUuidColumnTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulatePermTemplatesUsersTemplateUuidColumnTest.class, "schema.sql");

  private UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private DataChange underTest = new PopulatePermTemplatesUsersTemplateUuidColumn(db.database());

  @Test
  public void populate_uuids() throws SQLException {
    long permissionTemplateId_1 = 1L;
    String permissionTemplateUuid_1 = "uuid-1";
    insertPermissionTemplate(permissionTemplateId_1, permissionTemplateUuid_1);

    long permissionTemplateId_2 = 2L;
    String permissionTemplateUuid_2 = "uuid-2";
    insertPermissionTemplate(permissionTemplateId_2, permissionTemplateUuid_2);

    long permissionTemplateId_3 = 3L;
    String permissionTemplateUuid_3 = "uuid-3";
    insertPermissionTemplate(permissionTemplateId_3, permissionTemplateUuid_3);

    insertPermTemplatesUsers("4", permissionTemplateId_1);
    insertPermTemplatesUsers("5", permissionTemplateId_2);
    insertPermTemplatesUsers("6", permissionTemplateId_3);

    underTest.execute();

    assertThatPermTemplatesUsersTemplateUuidIsEqualTo("4", permissionTemplateUuid_1);
    assertThatPermTemplatesUsersTemplateUuidIsEqualTo("5", permissionTemplateUuid_2);
    assertThatPermTemplatesUsersTemplateUuidIsEqualTo("6", permissionTemplateUuid_3);
  }

  @Test
  public void delete_orphan_rows() throws SQLException {
    long permissionTemplateId_1 = 1L;
    String permissionTemplateUuid_1 = "uuid-1";
    insertPermissionTemplate(permissionTemplateId_1, permissionTemplateUuid_1);

    long permissionTemplateId_2 = 2L;
    String permissionTemplateUuid_2 = "uuid-2";
    insertPermissionTemplate(permissionTemplateId_2, permissionTemplateUuid_2);

    long permissionTemplateId_3 = 3L;
    String permissionTemplateUuid_3 = "uuid-3";
    insertPermissionTemplate(permissionTemplateId_3, permissionTemplateUuid_3);

    insertPermTemplatesUsers("4", permissionTemplateId_1);
    insertPermTemplatesUsers("5", permissionTemplateId_2);
    insertPermTemplatesUsers("6", 10L);

    assertThat(db.countRowsOfTable("perm_templates_users")).isEqualTo(3);

    underTest.execute();

    assertThatPermTemplatesUsersTemplateUuidIsEqualTo("4", permissionTemplateUuid_1);
    assertThatPermTemplatesUsersTemplateUuidIsEqualTo("5", permissionTemplateUuid_2);
    assertThat(db.countRowsOfTable("perm_templates_users")).isEqualTo(2);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    long permissionTemplateId_1 = 1L;
    String permissionTemplateUuid_1 = "uuid-1";
    insertPermissionTemplate(permissionTemplateId_1, permissionTemplateUuid_1);

    long permissionTemplateId_2 = 2L;
    String permissionTemplateUuid_2 = "uuid-2";
    insertPermissionTemplate(permissionTemplateId_2, permissionTemplateUuid_2);

    long permissionTemplateId_3 = 3L;
    String permissionTemplateUuid_3 = "uuid-3";
    insertPermissionTemplate(permissionTemplateId_3, permissionTemplateUuid_3);

    insertPermTemplatesUsers("4", permissionTemplateId_1);
    insertPermTemplatesUsers("5", permissionTemplateId_2);
    insertPermTemplatesUsers("6", permissionTemplateId_3);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThatPermTemplatesUsersTemplateUuidIsEqualTo("4", permissionTemplateUuid_1);
    assertThatPermTemplatesUsersTemplateUuidIsEqualTo("5", permissionTemplateUuid_2);
    assertThatPermTemplatesUsersTemplateUuidIsEqualTo("6", permissionTemplateUuid_3);
  }

  private void assertThatPermTemplatesUsersTemplateUuidIsEqualTo(String permTemplatesUsersUuid, String expectedUuid) {
    assertThat(db.select("select template_uuid from perm_templates_users where uuid = '" + permTemplatesUsersUuid + "'")
      .stream()
      .map(row -> row.get("TEMPLATE_UUID"))
      .findFirst())
        .hasValue(expectedUuid);
  }

  private void insertPermTemplatesUsers(String uuid, Long templateId) {
    db.executeInsert("perm_templates_users",
      "uuid", uuid,
      "user_id", 100,
      "template_id", templateId,
      "permission_reference", uuidFactory.create());
  }

  private void insertPermissionTemplate(Long id, String uuid) {
    db.executeInsert("permission_templates",
      "id", id,
      "uuid", uuid,
      "organization_uuid", id + 100,
      "name", uuidFactory.create(),
      "kee", uuidFactory.create());
  }

}
