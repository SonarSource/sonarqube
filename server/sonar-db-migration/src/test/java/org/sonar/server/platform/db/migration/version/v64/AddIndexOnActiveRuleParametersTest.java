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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

public class AddIndexOnActiveRuleParametersTest {

  private static final String TABLE = "active_rule_parameters";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddIndexOnActiveRuleParametersTest.class, "active_rule_parameters_without_index.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddIndexOnActiveRuleParameters underTest = new AddIndexOnActiveRuleParameters(db.database());

  @Test
  public void execute_adds_index() throws SQLException {
    underTest.execute();

    db.assertIndex(TABLE, "ix_arp_on_active_rule_id", "active_rule_id");
  }

  @Test
  public void execute_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute");

    underTest.execute();
  }
}
