/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v86;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

import static java.sql.Types.BOOLEAN;

public class MakeResetPasswordColumnNotNullTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeResetPasswordColumnNotNullTest.class, "schema.sql");

  private MigrationStep underTest = new MakeResetPasswordColumnNotNull(db.database());

  @Test
  public void execute_on_empty_db() throws SQLException {
    db.assertColumnDefinition("users", "reset_password", BOOLEAN, null, true);
    underTest.execute();
    db.assertColumnDefinition("users", "reset_password", BOOLEAN, null, false);
  }
}
