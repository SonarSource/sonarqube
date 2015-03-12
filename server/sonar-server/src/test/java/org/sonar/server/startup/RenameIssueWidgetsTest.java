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

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.db.IssueFilterDao;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.server.dashboard.db.WidgetDao;
import org.sonar.server.dashboard.db.WidgetPropertyDao;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class RenameIssueWidgetsTest {

  @Rule
  public DbTester dbTester = new DbTester();

  @Test
  public void should_rename_widgets() throws Exception {
    dbTester.prepareDbUnit(this.getClass(), "before.xml");

    doStart();

    dbTester.assertDbUnit(this.getClass(), "after.xml", "widgets", "widget_properties", "loaded_templates");
  }

  @Test
  public void should_skip_when_filter_removed() throws Exception {
    dbTester.prepareDbUnit(this.getClass(), "empty.xml");

    doStart();
  }

  @Test
  public void should_skip_when_already_executed() throws Exception {
    dbTester.prepareDbUnit(this.getClass(), "after.xml");

    doStart();
  }

  private void doStart() {
    System2 system2 = mock(System2.class);
    when(system2.now()).thenReturn(DateUtils.parseDateTime("2003-03-23T01:23:45+0100").getTime());

    RenameIssueWidgets task = new RenameIssueWidgets(
      new DbClient(
        dbTester.database(),
        dbTester.myBatis(),
        new WidgetDao(system2),
        new WidgetPropertyDao(system2),
        new IssueFilterDao(dbTester.myBatis()),
        new LoadedTemplateDao(dbTester.myBatis())
      ));

    task.start();
    task.stop();
  }
}
