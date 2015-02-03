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
import org.junit.experimental.categories.Category;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.db.FileSourceTesting;
import org.sonar.test.DbTests;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@Category(DbTests.class)
public class SourceLineResultSetIteratorTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(SourceLineResultSetIteratorTest.class, "schema.sql");

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
  public void parse_db_and_generate_source_line_documents() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    FileSourceTesting.updateDataColumn(connection, "FILE_UUID", FileSourceTesting.newFakeData(3).build());

    SourceLineResultSetIterator iterator = SourceLineResultSetIterator.create(dbClient, connection, 0L);
    assertThat(iterator.hasNext()).isTrue();
    SourceLineResultSetIterator.SourceFile file = iterator.next();
    assertThat(file.getLines()).hasSize(3);
    SourceLineDoc firstLine = file.getLines().get(0);
    assertThat(firstLine.projectUuid()).isEqualTo("PROJECT_UUID");
    assertThat(firstLine.fileUuid()).isEqualTo("FILE_UUID");
    assertThat(firstLine.line()).isEqualTo(1);
    assertThat(firstLine.scmRevision()).isEqualTo("REVISION_1");
    assertThat(firstLine.scmAuthor()).isEqualTo("AUTHOR_1");
    assertThat(firstLine.highlighting()).isEqualTo("HIGHLIGHTING_1");
    assertThat(firstLine.symbols()).isEqualTo("SYMBOLS_1");
    assertThat(firstLine.source()).isEqualTo("SOURCE_1");
    assertThat(firstLine.utLineHits()).isEqualTo(1);
    assertThat(firstLine.utConditions()).isEqualTo(2);
    assertThat(firstLine.utCoveredConditions()).isEqualTo(3);
    assertThat(firstLine.itLineHits()).isEqualTo(4);
    assertThat(firstLine.itConditions()).isEqualTo(5);
    assertThat(firstLine.itCoveredConditions()).isEqualTo(6);
    assertThat(firstLine.overallLineHits()).isEqualTo(7);
    assertThat(firstLine.overallConditions()).isEqualTo(8);
    assertThat(firstLine.overallCoveredConditions()).isEqualTo(9);
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
  public void should_fail_on_bad_data_format() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");

    FileSourceTesting.updateDataColumn(connection, "FILE_UUID", "THIS_IS_NOT_PROTOBUF".getBytes());

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
