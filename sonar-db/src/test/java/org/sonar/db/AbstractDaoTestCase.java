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
package org.sonar.db;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.test.DbTests;

/**
 * @deprecated use an instance of {@link DbTester instead} instead,
 * and do no forget to annotated the test class with {@link org.sonar.test.DbTests}.
 */
@Category(DbTests.class)
@Deprecated
public abstract class AbstractDaoTestCase {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  protected void setupData(String... testNames) {
    List<String> filenames = new ArrayList<>();
    for (String testName : testNames) {
      filenames.add(testName + (testName.endsWith(".xml") ? "" : ".xml"));
    }
    dbTester.prepareDbUnit(getClass(), filenames.toArray(new String[filenames.size()]));
  }

  protected void checkTables(String testName, String... tables) {
    checkTables(testName, new String[0], tables);
  }

  protected void checkTables(String testName, String[] excludedColumnNames, String... tables) {
    dbTester.assertDbUnit(getClass(), testName + (testName.endsWith("-result.xml") ? "" : "-result.xml"), excludedColumnNames, tables);
  }

  protected void checkTable(String testName, String table, String... columns) {
    dbTester.assertDbUnitTable(getClass(), testName + (testName.endsWith("-result.xml") ? "" : "-result.xml"), table, columns);
  }

  @Deprecated
  protected MyBatis getMyBatis() {
    return dbTester.myBatis();
  }
}
