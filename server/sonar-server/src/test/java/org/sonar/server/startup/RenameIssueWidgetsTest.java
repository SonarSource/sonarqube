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
package org.sonar.server.startup;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.dashboard.DashboardDao;
import org.sonar.db.dashboard.WidgetDao;
import org.sonar.db.dashboard.WidgetPropertyDao;
import org.sonar.db.issue.IssueFilterDao;
import org.sonar.db.loadedtemplate.LoadedTemplateDao;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class RenameIssueWidgetsTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Test
  public void should_rename_widgets() {
    dbTester.prepareDbUnit(this.getClass(), "before.xml");

    doStart();

    dbTester.assertDbUnit(this.getClass(), "after.xml", new String[] {"updated_at"}, "widgets", "widget_properties", "loaded_templates");

    List<Map<String, Object>> results = dbTester.select("select updated_at as \"updatedAt\" from widgets");
    assertThat(results).hasSize(6);
    // First widget is not updated
    assertThat(results.get(0).get("updatedAt")).isNull();
    for (int i = 1; i < results.size(); i++) {
      assertThat(results.get(i).get("updatedAt").toString()).startsWith("2003-03-2");
    }
  }

  @Test
  public void should_skip_when_already_executed() {
    dbTester.prepareDbUnit(this.getClass(), "after.xml");

    doStart();

    dbTester.assertDbUnit(this.getClass(), "after.xml", "widgets", "widget_properties", "loaded_templates");
  }

  private void doStart() {
    System2 system2 = mock(System2.class);
    Date now = DateUtils.parseDateTime("2003-03-23T01:23:45+0100");
    when(system2.now()).thenReturn(now.getTime());

    RenameIssueWidgets task = new RenameIssueWidgets(
      new DbClient(
        dbTester.database(),
        dbTester.myBatis(),
        new WidgetDao(dbTester.myBatis()),
        new WidgetPropertyDao(dbTester.myBatis()),
        new IssueFilterDao(dbTester.myBatis()),
        new LoadedTemplateDao(dbTester.myBatis()),
        new DashboardDao(dbTester.myBatis())
      ),
      system2,
      null);

    task.start();
    task.stop();
  }
}
