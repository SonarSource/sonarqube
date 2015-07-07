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
package org.sonar.server.issue.filter;

import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.issue.IssueFilterDao;
import org.sonar.db.loadedtemplate.LoadedTemplateDao;
import org.sonar.test.DbTests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class RegisterIssueFiltersTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  RegisterIssueFilters taskUnderTest;

  private System2 system;

  @Before
  public void setUp() {
    IssueFilterDao issueFilterDao = new IssueFilterDao(db.myBatis());
    LoadedTemplateDao loadedTemplateDao = new LoadedTemplateDao(db.myBatis());
    system = mock(System2.class);
    taskUnderTest = new RegisterIssueFilters(issueFilterDao, loadedTemplateDao, system);
  }

  @Test
  public void should_do_nothing_if_not_needed() {
    db.prepareDbUnit(getClass(), "do_nothing.xml");
    taskUnderTest.start();
    taskUnderTest.stop();
    db.assertDbUnit(getClass(), "do_nothing-result.xml", "issue_filters");
  }

  @Test
  public void should_register_issue_filters() {
    Date now = DateUtils.parseDateTime("2011-04-25T01:15:00+0200");
    when(system.now()).thenReturn(now.getTime());

    db.prepareDbUnit(getClass(), "empty.xml");
    taskUnderTest.start();
    taskUnderTest.stop();
    db.assertDbUnit(getClass(), "register-result.xml", new String[]{"created_at", "updated_at"}, "issue_filters", "loaded_templates");

  }
}
