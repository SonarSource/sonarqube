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
import java.sql.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;

public class AddTemplateUuidColumnToPermTemplatesUsersTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddTemplateUuidColumnToPermTemplatesUsersTest.class, "schema.sql");

  private UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private DdlChange underTest = new AddTemplateUuidColumnToPermTemplatesUsers(db.database());

  @Before
  public void setup() {
    insertPermTemplatesUsers("1");
    insertPermTemplatesUsers("2");
    insertPermTemplatesUsers("3");
  }

  @Test
  public void add_uuid_column_to_permission_templates() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("perm_templates_users", "template_uuid", Types.VARCHAR, 40, true);

    assertThat(db.countRowsOfTable("perm_templates_users"))
      .isEqualTo(3);
  }

  private void insertPermTemplatesUsers(String uuid) {
    db.executeInsert("perm_templates_users",
      "uuid", uuid,
      "user_id", 100,
      "template_id", 200,
      "permission_reference", uuidFactory.create());
  }
}
