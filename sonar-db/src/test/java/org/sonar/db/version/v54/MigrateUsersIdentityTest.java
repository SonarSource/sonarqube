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

public class MigrateUsersIdentityTest {

  static final String TABLE = "users";

  static final long NOW = 1500000000000L;

  final System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, MigrateUsersIdentityTest.class, "schema.sql");

  MigrationStep migration;

  @Before
  public void setUp() {
    db.executeUpdateSql("truncate table " + TABLE);
    when(system2.now()).thenReturn(NOW);
    migration = new MigrateUsersIdentity(db.database(), system2);
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
    assertUser(101, "john", NOW);
    assertUser(102, "arthur", NOW);
  }

  @Test
  public void nothing_to_do_on_already_migrated_data() throws Exception {
    db.prepareDbUnit(this.getClass(), "migrate-result.xml");

    migration.execute();

    assertThat(db.countRowsOfTable(TABLE)).isEqualTo(2);
    assertUser(101, "john", 1418215735485L);
    assertUser(102, "arthur", 1418215735485L);
  }

  private void assertUser(long userId, String expectedAuthorityId, long expectedUpdatedAt) {
    Map<String, Object> result = db.selectFirst("SELECT u.external_identity as \"externalIdentity\", " +
      "u.external_identity_provider as \"externalIdentityProvider\", " +
      "u.updated_at as \"updatedAt\" " +
      "FROM users u WHERE u.id=" + userId);
    assertThat(result.get("externalIdentity")).isEqualTo(expectedAuthorityId);
    assertThat(result.get("externalIdentityProvider")).isEqualTo("sonarqube");
    assertThat(result.get("updatedAt")).isEqualTo(expectedUpdatedAt);
  }

}
