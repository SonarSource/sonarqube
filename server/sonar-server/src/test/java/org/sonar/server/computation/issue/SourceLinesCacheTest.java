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

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.source.db.FileSourceDao;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class SourceLinesCacheTest {

  @Rule
  public DbTester dbTester = new DbTester();

  @Test
  public void load_data() throws Exception {
    dbTester.prepareDbUnit(getClass(), "load_data.xml");
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new FileSourceDao(dbTester.myBatis()));
    SourceLinesCache cache = new SourceLinesCache(dbClient);
    cache.init("FILE_A");

    // load data on demand -> still nothing in cache
    assertThat(cache.countLines()).isEqualTo(0);

    assertThat(cache.lineAuthor(1)).isEqualTo("charlie");
    assertThat(cache.lineAuthor(2)).isEqualTo("cabu");

    // blank author -> return null
    assertThat(cache.lineAuthor(3)).isNull();

    // only 3 lines in the file
    assertThat(cache.lineAuthor(100)).isNull();

    assertThat(cache.countLines()).isEqualTo(3);

    cache.clear();
    assertThat(cache.countLines()).isEqualTo(0);
  }


}
