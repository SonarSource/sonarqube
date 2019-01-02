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
package org.sonar.server.platform.db.migration.version.v72;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class IncreaseCryptedPasswordSizeTest {
  private static final String TABLE_NAME = "users";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(IncreaseCryptedPasswordSizeTest.class, "users.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private IncreaseCryptedPasswordSize underTest = new IncreaseCryptedPasswordSize(db.database());

  @Test
  public void cannot_insert_crypted_password() {
    expectedException.expect(IllegalStateException.class);

    insertRow();
  }

  @Test
  public void can_insert_crypted_password_after_execute() throws SQLException {
    underTest.execute();
    assertThat(db.countRowsOfTable(TABLE_NAME)).isEqualTo(0);
    insertRow();
    assertThat(db.countRowsOfTable(TABLE_NAME)).isEqualTo(1);
  }

  private void insertRow() {
    // bcrypt hash is 60 characters
    db.executeInsert(
      "USERS",
      "CRYPTED_PASSWORD", "$2a$10$8tscphgcElKF5vOBer4H.OVfLKpPIH74hK.rxyhOP5HVyZHyfgRGy",
      "IS_ROOT", false,
      "ONBOARDED", false);
  }

}
