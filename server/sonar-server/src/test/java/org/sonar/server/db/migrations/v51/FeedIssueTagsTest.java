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

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbTester;

import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedIssueTagsTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(FeedIssueTagsTest.class, "schema.sql");

  FeedIssueTags migration;

  System2 system;

  @Before
  public void setUp() throws Exception {
    db.executeUpdateSql("truncate table rules");
    db.executeUpdateSql("truncate table issues");

    system = mock(System2.class);
    Date now = DateUtils.parseDateTime("2014-12-08T17:33:00+0100");
    when(system.now()).thenReturn(now.getTime());
    migration = new FeedIssueTags(db.database(), system);
  }

  @Test
  public void migrate_empty_db() throws Exception {
    migration.execute();
  }

  @Test
  public void migrate_with_rule_tags() throws Exception {
    db.prepareDbUnit(this.getClass(), "before.xml");
    migration.execute();
    db.assertDbUnit(this.getClass(), "after-result.xml", "issues");
  }
}
