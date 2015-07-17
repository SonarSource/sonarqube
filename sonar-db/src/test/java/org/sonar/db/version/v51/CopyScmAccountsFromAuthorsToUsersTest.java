/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.db.version.v51;

import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.version.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CopyScmAccountsFromAuthorsToUsersTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, CopyScmAccountsFromAuthorsToUsersTest.class, "schema.sql");

  MigrationStep migration;
  System2 system = mock(System2.class);

  @Before
  public void setUp() {
    db.executeUpdateSql("truncate table authors");
    db.executeUpdateSql("truncate table users");
    migration = new CopyScmAccountsFromAuthorsToUsers(db.database(), system);
  }

  @Test
  public void migrate() throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");
    Long oldDate = 1500000000000L;
    Long updatedDate = 2000000000000L;
    when(system.now()).thenReturn(updatedDate);

    migration.execute();

    User simon = getUserByLogin("simon");
    assertThat(simon.scmAccounts).isEqualTo(UserDto.SCM_ACCOUNTS_SEPARATOR + "Simon B" + UserDto.SCM_ACCOUNTS_SEPARATOR + "simon@codehaus.org" + UserDto.SCM_ACCOUNTS_SEPARATOR);
    assertThat(simon.updatedAt).isEqualTo(updatedDate);

    User fabrice = getUserByLogin("fabrice");
    assertThat(fabrice.scmAccounts).isEqualTo(UserDto.SCM_ACCOUNTS_SEPARATOR + "fab" + UserDto.SCM_ACCOUNTS_SEPARATOR);
    assertThat(fabrice.updatedAt).isEqualTo(updatedDate);

    assertThat(getUserByLogin("julien").updatedAt).isEqualTo(oldDate);
    assertThat(getUserByLogin("jb").updatedAt).isEqualTo(oldDate);
    assertThat(getUserByLogin("disable").updatedAt).isEqualTo(oldDate);
    assertThat(getUserByLogin("teryk").updatedAt).isEqualTo(oldDate);
    assertThat(getUserByLogin("teryk2").updatedAt).isEqualTo(oldDate);
  }

  @Test
  public void nothing_to_migrate_when_no_authors() throws Exception {
    db.prepareDbUnit(getClass(), "no_authors.xml");
    Long oldDate = 1500000000000L;
    Long updatedDate = 2000000000000L;
    when(system.now()).thenReturn(updatedDate);

    migration.execute();

    assertThat(db.countSql("SELECT count(*) FROM USERS WHERE updated_at=" + updatedDate)).isEqualTo(0);
    assertThat(db.countSql("SELECT count(*) FROM USERS WHERE updated_at=" + oldDate)).isEqualTo(7);
  }

  private User getUserByLogin(String login) {
    return new User(db.selectFirst("SELECT u.scm_Accounts as \"scmAccounts\", u.updated_at as \"updatedAt\" FROM users u WHERE u.login='" + login + "'"));
  }

  private static class User {
    String scmAccounts;
    Long updatedAt;

    User(Map<String, Object> map) {
      scmAccounts = (String) map.get("scmAccounts");
      updatedAt = (Long) map.get("updatedAt");
    }
  }

}
