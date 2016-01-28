/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v54;

import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MigrateDisabledUsersToOnlyKeepLoginAndNameTest {

  static final String TABLE = "users";

  static final long NOW = 1500000000000L;

  final System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, MigrateDisabledUsersToOnlyKeepLoginAndNameTest.class, "schema.sql");

  MigrationStep migration;

  @Before
  public void setUp() {
    db.executeUpdateSql("truncate table " + TABLE);
    when(system2.now()).thenReturn(NOW);
    migration = new MigrateDisabledUsersToOnlyKeepLoginAndName(db.database(), system2);
  }

  @Test
  public void migrate_empty_db() throws Exception {
    migration.execute();
  }

  @Test
  public void migrate() throws Exception {
    db.prepareDbUnit(this.getClass(), "migrate.xml");

    migration.execute();

    assertThat(db.countRowsOfTable(TABLE)).isEqualTo(2);
    verifyUser(101, false, 1418215735485L);
    verifyUser(102, true, NOW);
  }

  @Test
  public void nothing_to_do_on_already_migrated_data() throws Exception {
    db.prepareDbUnit(this.getClass(), "already-migrated-data.xml");

    migration.execute();

    assertThat(db.countRowsOfTable(TABLE)).isEqualTo(2);
    verifyUser(101, false, 1418215735485L);
    verifyUser(102, true, 1418215735485L);
  }

  private void verifyUser(long userId, boolean isDisable, long expectedUpdatedAt) {
    Map<String, Object> result = db.selectFirst("SELECT u.email as \"email\",  u.scm_accounts as \"scmAccounts\"," +
      "u.external_identity as \"externalIdentity\", u.external_identity_provider as \"externalIdentityProvider\", " +
      "u.salt as \"salt\", u.crypted_password as \"cryptedPassword\", " +
      "u.remember_token as \"rememberToken\", u.remember_token_expires_at as \"rememberTokenExpiresAt\", " +
      "u.updated_at as \"updatedAt\" " +
      "FROM users u WHERE u.id=" + userId);
    if (isDisable) {
      assertThat(result.get("email")).isNull();
      assertThat(result.get("scmAccounts")).isNull();
      assertThat(result.get("externalIdentity")).isNull();
      assertThat(result.get("externalIdentityProvider")).isNull();
      assertThat(result.get("salt")).isNull();
      assertThat(result.get("cryptedPassword")).isNull();
      assertThat(result.get("rememberToken")).isNull();
      assertThat(result.get("rememberTokenExpiresAt")).isNull();
      assertThat(result.get("updatedAt")).isEqualTo(expectedUpdatedAt);
    } else {
      assertThat(result.get("email")).isNotNull();
      assertThat(result.get("scmAccounts")).isNotNull();
      assertThat(result.get("externalIdentity")).isNotNull();
      assertThat(result.get("externalIdentityProvider")).isNotNull();
      assertThat(result.get("salt")).isNotNull();
      assertThat(result.get("cryptedPassword")).isNotNull();
      assertThat(result.get("rememberToken")).isNotNull();
      assertThat(result.get("rememberTokenExpiresAt")).isNotNull();
      assertThat(result.get("updatedAt")).isEqualTo(expectedUpdatedAt);
    }
  }
}
