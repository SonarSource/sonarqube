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
package org.sonar.server.platform.db.migration.version.v77;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

public class AddMembersSyncFlagToOrgAlmBindingTest {

  private static final String TABLE = "organization_alm_bindings";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(AddMembersSyncFlagToOrgAlmBindingTest.class, "org_alm_binding.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddMembersSyncFlagToOrgAlmBinding underTest = new AddMembersSyncFlagToOrgAlmBinding(db.database());

  @Test
  public void add_column() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition(TABLE, "members_sync_enabled", Types.BOOLEAN, null);
  }

  @Test
  public void migration_is_not_re_entrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);

    underTest.execute();
  }
}
