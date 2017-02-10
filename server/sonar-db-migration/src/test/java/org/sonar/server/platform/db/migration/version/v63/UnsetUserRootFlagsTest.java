/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;

public class UnsetUserRootFlagsTest {

  private static final long CREATED_AT = 1_500L;
  private static final long FIXED_AT = 1_600L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TestSystem2 system = new TestSystem2().setNow(FIXED_AT);

  @Rule
  public DbTester db = DbTester.createForSchema(system, UnsetUserRootFlagsTest.class, "in_progress_users.sql");

  private UnsetUserRootFlags underTest = new UnsetUserRootFlags(db.database(), system);

  @Test
  public void sets_USERS_IS_ROOT_to_false() throws SQLException {
    UserDto root1 = db.users().makeRoot(createUser());
    UserDto user1 = createUser();
    UserDto root2 = db.users().makeRoot(createUser());
    UserDto user2 = createUser();

    underTest.execute();

    verifyNotRoot(CREATED_AT, user1, user2);
    verifyNotRoot(FIXED_AT, root1, root2);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    UserDto root = db.users().makeRoot(createUser());

    underTest.execute();
    verifyNotRoot(FIXED_AT, root);

    system.setNow(FIXED_AT + 100L);
    underTest.execute();
    verifyNotRoot(FIXED_AT, root);
  }

  private void verifyNotRoot(long updatedAt, UserDto... users) {
    for (UserDto user : users) {
      db.rootFlag().verify(user, false, updatedAt);
    }
  }

  private UserDto createUser() {
    return db.users().insertUser(UserTesting.newUserDto()
      .setCreatedAt(CREATED_AT)
      .setUpdatedAt(CREATED_AT));
  }
}
