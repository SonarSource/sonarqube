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

package org.sonar.server.db.migrations.v50;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.TestDatabase;
import org.sonar.server.db.migrations.DatabaseMigration;

import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedFileSourcesMigrationTest {

  @ClassRule
  public static TestDatabase db = new TestDatabase().schema(FeedFileSourcesMigrationTest.class, "schema.sql");

  DatabaseMigration migration;

  System2 system;

  @Before
  public void setUp() throws Exception {
    db.executeUpdateSql("truncate table metrics");
    db.executeUpdateSql("truncate table snapshots");
    db.executeUpdateSql("truncate table snapshot_sources");
    db.executeUpdateSql("truncate table projects");
    db.executeUpdateSql("truncate table project_measures");
    db.executeUpdateSql("truncate table file_sources");

    system = mock(System2.class);
    Date now = DateUtils.parseDateTime("2014-11-17T16:27:00+0100");
    when(system.now()).thenReturn(now.getTime());
    migration = new FeedFileSources(db.database(), system);
  }

  @Test
  public void migrate_sources_with_no_scm() throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");

    db.executeUpdateSql("insert into snapshot_sources " +
      "(snapshot_id, data, updated_at) " +
      "values " +
      "(6, 'class Foo {\r\n  // Empty\r\n}\r\n', '2014-10-31 16:44:02.000')");

    migration.execute();

    db.assertDbUnit(getClass(), "after.xml", "file_sources");
  }

  @Test
  public void migrate_sources_with_scm() throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");

    db.executeUpdateSql("insert into snapshot_sources " +
      "(snapshot_id, data, updated_at) " +
      "values " +
      "(6, 'class Foo {\r\n  // Empty\r\n}\r\n', '2014-10-31 16:44:02.000')");

    db.executeUpdateSql("insert into project_measures " +
      "(metric_id, snapshot_id, text_value) " +
      "values " +
      "(1, 6, $$1=aef12a;2=abe465;3=afb789;4=afb789$$)");

    db.executeUpdateSql("insert into project_measures " +
      "(metric_id, snapshot_id, text_value) " +
      "values " +
      "(2, 6, $$1=alice;2=bob;3=carol;4=carol$$)");

    db.executeUpdateSql("insert into project_measures " +
      "(metric_id, snapshot_id, text_value) " +
      "values " +
      "(3, 6, $$1=2014-04-25T12:34:56+0100;2=2014-07-25T12:34:56+0100;3=2014-03-23T12:34:56+0100;4=2014-03-23T12:34:56+0100$$)");

    migration.execute();

    db.assertDbUnit(getClass(), "after-with-scm.xml", "file_sources");
  }
}
