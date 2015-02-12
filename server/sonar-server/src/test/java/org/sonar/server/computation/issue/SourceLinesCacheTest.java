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
package org.sonar.server.computation.issue;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.source.db.FileSourceDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.server.source.db.FileSourceTesting;
import org.sonar.test.DbTests;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class SourceLinesCacheTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
  }

  @Test
  public void line_author() throws Exception {
    dbTester.prepareDbUnit(getClass(), "load_data.xml");
    FileSourceDb.Data.Builder data = FileSourceDb.Data.newBuilder();
    data.addLinesBuilder().setLine(1).setScmAuthor("charb").setScmDate(1_400_000_000_000L);
    data.addLinesBuilder().setLine(2).setScmAuthor("cabu").setScmDate(1_500_000_000_000L);
    data.addLinesBuilder().setLine(3).setScmAuthor("wolinski").setScmDate(1_300_000_000_000L);
    data.addLinesBuilder().setLine(4);
    try (Connection connection = dbTester.openConnection()) {
      FileSourceTesting.updateDataColumn(connection, "FILE_A", data.build());
    }

    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new FileSourceDao(dbTester.myBatis()));
    SourceLinesCache cache = new SourceLinesCache(dbClient);
    cache.init("FILE_A");

    // load data on demand -> still nothing in cache
    assertThat(cache.countLines()).isEqualTo(0);

    assertThat(cache.lineAuthor(1)).isEqualTo("charb");
    assertThat(cache.lineAuthor(2)).isEqualTo("cabu");
    assertThat(cache.lineAuthor(3)).isEqualTo("wolinski");

    // blank author on line 4 -> return last committer on file
    assertThat(cache.lineAuthor(4)).isEqualTo("cabu");

    // only 4 lines in the file -> return last committer on file
    assertThat(cache.lineAuthor(100)).isEqualTo("cabu");

    assertThat(cache.countLines()).isEqualTo(4);

    cache.clear();
    assertThat(cache.countLines()).isEqualTo(0);
  }

}
