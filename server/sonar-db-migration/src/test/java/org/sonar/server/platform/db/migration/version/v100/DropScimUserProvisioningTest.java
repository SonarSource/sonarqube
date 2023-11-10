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
package org.sonar.server.platform.db.migration.version.v100;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.platform.db.migration.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class DropScimUserProvisioningTest {

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(DropScimUserProvisioning.class);
  private final DataChange underTest = new DropScimUserProvisioning(db.database());

  @Test
  public void migration_should_truncate_scim_users_table() throws SQLException {
    insertScimUser(1);
    insertScimUser(2);

    underTest.execute();

    assertThat(db.select("select * from scim_users")).isEmpty();
  }

  private void insertScimUser(long id) {
    db.executeInsert("scim_users",
      "scim_uuid", "any-scim-uuid-" + id,
      "user_uuid", "any-user-uuid-" + id
    );
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertScimUser(1);
    insertScimUser(2);

    underTest.execute();
    underTest.execute();
    assertThat(db.select("select * from scim_users")).isEmpty();
  }

}
