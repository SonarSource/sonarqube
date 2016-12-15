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
package org.sonar.db.version;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseVersionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private DatabaseVersion underTest = new DatabaseVersion(dbClient);

  @Test
  public void getVersion() {
    dbTester.getDbClient().schemaMigrationDao().insert(dbSession, "1");
    dbTester.getDbClient().schemaMigrationDao().insert(dbSession, "2");
    dbTester.getDbClient().schemaMigrationDao().insert(dbSession, "4");
    dbTester.getDbClient().schemaMigrationDao().insert(dbSession, "123");
    dbTester.getDbClient().schemaMigrationDao().insert(dbSession, "50");
    dbSession.commit();

    Integer version = underTest.getVersion();

    assertThat(version).isEqualTo(123);
  }

  @Test
  public void getVersion_no_rows() {
    Integer version = underTest.getVersion();

    assertThat(version).isNull();
  }

  @Test
  public void getStatus() {
    assertThat(DatabaseVersion.getStatus(null, 150)).isEqualTo(DatabaseVersion.Status.FRESH_INSTALL);
    assertThat(DatabaseVersion.getStatus(123, 150)).isEqualTo(DatabaseVersion.Status.REQUIRES_UPGRADE);
    assertThat(DatabaseVersion.getStatus(150, 150)).isEqualTo(DatabaseVersion.Status.UP_TO_DATE);
    assertThat(DatabaseVersion.getStatus(200, 150)).isEqualTo(DatabaseVersion.Status.REQUIRES_DOWNGRADE);
  }
}
