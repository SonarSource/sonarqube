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
package org.sonar.db.version.v52;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

public class FeedManualMeasuresComponentUuidTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, FeedManualMeasuresComponentUuidTest.class, "schema.sql");

  FeedManualMeasuresComponentUuid underTest;

  @Before
  public void setUp() {
    db.executeUpdateSql("truncate table manual_measures");
    db.executeUpdateSql("truncate table projects");

    underTest = new FeedManualMeasuresComponentUuid(db.database());
  }

  @Test
  public void migrate_empty_db() throws Exception {
    underTest.execute();
  }

  @Test
  public void migrate() throws Exception {
    db.prepareDbUnit(this.getClass(), "migrate.xml");
    underTest.execute();
    db.assertDbUnit(this.getClass(), "migrate-result.xml", "manual_measures");
  }
}
