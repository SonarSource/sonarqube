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
package org.sonar.server.es;

import org.apache.commons.dbutils.DbUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.TestDatabase;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueDoc;

import java.sql.Connection;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class IssueResultSetIteratorTest {

  @Rule
  public TestDatabase dbTester = new TestDatabase();

  DbClient client;
  Connection connection;

  @Before
  public void setUp() throws Exception {
    client = new DbClient(dbTester.database(), dbTester.myBatis());
    connection = dbTester.openConnection();
  }

  @After
  public void tearDown() throws Exception {
    DbUtils.closeQuietly(connection);
  }

  @Test
  public void iterator_over_issues() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    IssueResultSetIterator it = IssueResultSetIterator.create(client, connection, 0L);

    assertThat(it.hasNext()).isTrue();
    IssueDoc issue = it.next();
    assertThat(issue.key()).isNotEmpty();
    assertThat(issue.assignee()).isNotEmpty();
    assertThat(issue.componentUuid()).isNotEmpty();
    assertThat(issue.projectUuid()).isNotEmpty();
    assertThat(issue.debt().toMinutes()).isGreaterThan(0L);

    assertThat(it.hasNext()).isTrue();
    issue = it.next();
    assertThat(issue.key()).isNotEmpty();
    assertThat(issue.assignee()).isNotEmpty();
    assertThat(issue.componentUuid()).isNotEmpty();
    assertThat(issue.projectUuid()).isNotEmpty();
    assertThat(issue.debt().toMinutes()).isGreaterThan(0L);

    assertThat(it.hasNext()).isFalse();
  }

  @Test
  public void select_after_date() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    Date date = DateUtils.parseDate("2014-01-01");
    IssueResultSetIterator it = IssueResultSetIterator.create(client, connection, date.getTime());

    assertThat(it.hasNext()).isTrue();
    IssueDoc issue = it.next();
    assertThat(issue.key()).isEqualTo("DEF");

    assertThat(it.hasNext()).isFalse();
  }
}
