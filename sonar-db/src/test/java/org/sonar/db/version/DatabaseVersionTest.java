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
package org.sonar.db.version;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class DatabaseVersionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Test
  public void getVersion() {
    dbTester.prepareDbUnit(getClass(), "getVersion.xml");

    Integer version = new DatabaseVersion(dbTester.myBatis()).getVersion();

    assertThat(version).isEqualTo(123);
  }

  @Test
  public void getVersion_no_rows() {
    dbTester.prepareDbUnit(getClass(), "getVersion_no_rows.xml");

    Integer version = new DatabaseVersion(dbTester.myBatis()).getVersion();

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
