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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.persistence.TestDatabase;
import org.sonar.server.db.DbClient;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class SourceLineResultSetIteratorTest {

  @ClassRule
  public static TestDatabase db = new TestDatabase().schema(SourceLineResultSetIteratorTest.class, "schema.sql");

  DbClient dbClient;

  Connection connection;

  @Before
  public void setUp() throws Exception {
    dbClient = new DbClient(db.database(), db.myBatis());
    connection = db.openConnection();
  }

  @After
  public void after() throws Exception {
    connection.close();
  }

  @Test
  public void should_generate_source_line_documents() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    PreparedStatement stmt = connection.prepareStatement("UPDATE file_sources SET data = ? WHERE id=1");
    stmt.setString(1, "aef12a,alice,2014-04-25T12:34:56+0100,1,0,0,2,0,0,3,0,0,polop,palap,,class Foo {\r\n" +
      "abe465,bob,2014-07-25T12:34:56+0100,,,,,,,,,,,,,  // Empty\r\n" +
      "afb789,carol,2014-03-23T12:34:56+0100,,,,,,,,,,,,,}\r\n" +
      "afb789,carol,2014-03-23T12:34:56+0100,,,,,,,,,,,,,\r\n");
    stmt.executeUpdate();

    SourceLineResultSetIterator iterator = SourceLineResultSetIterator.create(dbClient, connection, 0L);
    assertThat(iterator.hasNext()).isTrue();
    SourceLineResultSetIterator.SourceFile file = iterator.next();
    assertThat(file.getLines()).hasSize(4);
    SourceLineDoc firstLine = file.getLines().get(0);
    assertThat(firstLine.projectUuid()).isEqualTo("uuid-MyProject");
    assertThat(firstLine.fileUuid()).isEqualTo("uuid-MyFile.xoo");
    assertThat(firstLine.line()).isEqualTo(1);
    assertThat(firstLine.scmRevision()).isEqualTo("aef12a");
    assertThat(firstLine.scmAuthor()).isEqualTo("alice");
    // TODO Sanitize usage of fscking dates
    // assertThat(firstLine.scmDate()).isEqualTo(DateUtils.parseDateTime("2014-04-25T12:34:56+0100"));
    assertThat(firstLine.highlighting()).isEqualTo("polop");
    assertThat(firstLine.symbols()).isEqualTo("palap");
    assertThat(firstLine.source()).isEqualTo("class Foo {");
    assertThat(firstLine.utLineHits()).isEqualTo(1);
    assertThat(firstLine.utConditions()).isEqualTo(0);
    assertThat(firstLine.utCoveredConditions()).isEqualTo(0);
    assertThat(firstLine.itLineHits()).isEqualTo(2);
    assertThat(firstLine.itConditions()).isEqualTo(0);
    assertThat(firstLine.itCoveredConditions()).isEqualTo(0);
    assertThat(firstLine.overallLineHits()).isEqualTo(3);
    assertThat(firstLine.overallConditions()).isEqualTo(0);
    assertThat(firstLine.overallCoveredConditions()).isEqualTo(0);
    iterator.close();
  }

  @Test
  public void should_ignore_lines_already_handled() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");

    SourceLineResultSetIterator iterator = SourceLineResultSetIterator.create(dbClient, connection, 2000000000000L);
    assertThat(iterator.hasNext()).isFalse();
    iterator.close();
  }

  @Test
  public void parse_empty_file() throws Exception {
    db.prepareDbUnit(getClass(), "empty-file.xml");

    SourceLineResultSetIterator iterator = SourceLineResultSetIterator.create(dbClient, connection, 0L);
    assertThat(iterator.hasNext()).isTrue();
    SourceLineResultSetIterator.SourceFile file = iterator.next();
    assertThat(file.getFileUuid()).isEqualTo("uuid-MyFile.xoo");
    assertThat(file.getLines()).isEmpty();
    iterator.close();
  }

  @Test
  public void should_fail_on_bad_csv() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    PreparedStatement stmt = connection.prepareStatement("UPDATE file_sources SET data = ? WHERE id=1");
    stmt.setString(1, "plouf");
    stmt.executeUpdate();
    stmt.close();

    SourceLineResultSetIterator iterator = SourceLineResultSetIterator.create(dbClient, connection, 0L);
    try {
      assertThat(iterator.hasNext()).isTrue();
      iterator.next();
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
    iterator.close();
  }
}
