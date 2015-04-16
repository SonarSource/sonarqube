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

package org.sonar.server.db.migrations.v51;

import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.migrations.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.parseDate;

public class FeedIssuesLongDatesTest {
  @ClassRule
  public static DbTester db = new DbTester().schema(FeedIssuesLongDatesTest.class, "schema.sql");

  @Test
  public void execute() throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");

    MigrationStep migration = new FeedIssuesLongDates(db.database(), System2.INSTANCE);
    migration.execute();

    int count = db
      .countSql("select count(*) from issues where " +
        "issue_creation_date_ms is not null " +
        "and issue_update_date_ms is not null " +
        "and issue_close_date_ms is not null");
    assertThat(count).isEqualTo(2);
  }

  @Test
  public void take_now_if_date_in_the_future() throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");
    System2 system2 = mock(System2.class);
    when(system2.now()).thenReturn(0L);

    MigrationStep migration = new FeedIssuesLongDates(db.database(), mock(System2.class));
    migration.execute();

    int count = db
      .countSql("select count(*) from issues where " +
        "issue_creation_date_ms = 0");
    assertThat(count).isEqualTo(1);
  }

  @Test
  public void take_snapshot_date_if_in_the_past() throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");
    long snapshotTime = parseDate("2014-09-25").getTime();

    MigrationStep migration = new FeedIssuesLongDates(db.database(), System2.INSTANCE);
    migration.execute();

    int count = db
      .countSql("select count(*) from issues where " +
        "issue_creation_date_ms=" + snapshotTime);
    assertThat(count).isEqualTo(1);
  }
}
