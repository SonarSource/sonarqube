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
package org.sonar.server.source.index;

import com.google.common.collect.Lists;
import org.apache.commons.io.Charsets;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.TestDatabase;
import org.sonar.server.db.DbClient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class SourceLineResultSetIteratorTest {

  @ClassRule
  public static TestDatabase db = new TestDatabase();

  DbClient dbClient;

  Connection connection;

  @Before
  public void setUp() throws Exception {
    dbClient = new DbClient(db.database(), db.myBatis());
    db.schema(this.getClass(), "schema.sql");
    connection = db.openConnection();
  }

  @After
  public void after() throws Exception {
    connection.close();
  }

  @Test
  public void should_generate_source_line_documents() throws Exception {
    db.prepareDbUnit(getClass(), "source-with-scm.xml");
    Connection connection = db.openConnection();
    PreparedStatement stmt = connection.prepareStatement("UPDATE file_sources SET data = ? WHERE id=1");
    stmt.setBytes(1, ("aef12a,alice,2014-04-25T12:34:56+0100,,class Foo {\r\n" +
      "abe465,bob,2014-07-25T12:34:56+0100,,  // Empty\r\n" +
      "afb789,carol,2014-03-23T12:34:56+0100,,}\r\n" +
      "afb789,carol,2014-03-23T12:34:56+0100,,\r\n").getBytes(Charsets.UTF_8));
    stmt.executeUpdate();

    SourceLineResultSetIterator iterator = SourceLineResultSetIterator.create(dbClient, connection, 0L);
    assertThat(iterator.hasNext()).isTrue();
    List<SourceLineDoc> sourceLines = Lists.newArrayList(iterator.next());
    assertThat(sourceLines).hasSize(4);
    SourceLineDoc firstLine = sourceLines.get(0);
    assertThat(firstLine.projectUuid()).isEqualTo("uuid-MyProject");
    assertThat(firstLine.fileUuid()).isEqualTo("uuid-MyFile.xoo");
    assertThat(firstLine.line()).isEqualTo(1);
    assertThat(firstLine.scmRevision()).isEqualTo("aef12a");
    assertThat(firstLine.scmAuthor()).isEqualTo("alice");
    assertThat(firstLine.scmDate()).isEqualTo(DateUtils.parseDateTime("2014-04-25T12:34:56+0100"));
    assertThat(firstLine.highlighting()).isEmpty();
    assertThat(firstLine.source()).isEqualTo("class Foo {");
  }

  @Test
  public void should_ignore_lines_already_handled() throws Exception {
    db.prepareDbUnit(getClass(), "source-with-scm.xml");

    SourceLineResultSetIterator iterator = SourceLineResultSetIterator.create(dbClient, db.openConnection(),
      DateUtils.parseDateTime("2014-11-01T16:44:02+0100").getTime());
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  public void should_fail_on_bad_csv() throws Exception {
    db.prepareDbUnit(getClass(), "source-with-scm.xml");
    Connection connection = db.openConnection();
    PreparedStatement stmt = connection.prepareStatement("UPDATE file_sources SET data = ? WHERE id=1");
    stmt.setBytes(1, ("plouf").getBytes(Charsets.UTF_8));
    stmt.executeUpdate();
    connection.commit();

    SourceLineResultSetIterator iterator = SourceLineResultSetIterator.create(dbClient, connection, 0L);
    assertThat(iterator.hasNext()).isTrue();
    iterator.next();
  }

}
