/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.persistence.dao;

import com.google.common.collect.Lists;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.persistence.InMemoryDatabase;
import org.sonar.persistence.MyBatis;
import org.sonar.persistence.model.DuplicationUnit;

import java.util.List;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class DuplicationDaoTest {
  protected static IDatabaseTester databaseTester;
  private static InMemoryDatabase database;
  private static DuplicationDao dao;

  @BeforeClass
  public static void startDatabase() throws Exception {
    database = new InMemoryDatabase();
    MyBatis myBatis = new MyBatis(database);

    database.start();
    myBatis.start();

    dao = new DuplicationDao(myBatis);
    databaseTester = new DataSourceDatabaseTester(database.getDataSource());
  }

  @AfterClass
  public static void stopDatabase() throws Exception {
    if (databaseTester != null) {
      databaseTester.onTearDown();
    }
    database.stop();
  }

  @Test
  public void testBatchInsert() {
    List<DuplicationUnit> duplications = Lists.newArrayList();
    for (int i = 0; i < 50; i++) {
      duplications.add(new DuplicationUnit(i, i, "hash", 2, 30, 40));
    }
    dao.insert(duplications);

    for (DuplicationUnit duplication : duplications) {
      // batch insert : faster but generated ids are not returned
      assertThat(duplication.getId(), nullValue());
    }
  }
}
