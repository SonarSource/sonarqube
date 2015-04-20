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

package org.sonar.server.db.migrations.v52;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.migrations.MigrationStep;

public class FeedFileSourcesDataTypeTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(FeedFileSourcesDataTypeTest.class, "schema.sql");

  MigrationStep migration;

  @Before
  public void setUp() throws Exception {
    db.executeUpdateSql("truncate table file_sources");

    migration = new FeedFileSourcesDataType(db.database());
  }

  @Test
  public void migrate_empty_db() throws Exception {
    migration.execute();
  }

  @Test
  public void migrate() throws Exception {
    db.prepareDbUnit(this.getClass(), "migrate.xml");
    migration.execute();
    db.assertDbUnit(this.getClass(), "migrate-result.xml", "file_sources");
  }
}
