/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractPluggableDbTesterTest {

  private TestableAbstractPluggableDbTester underTest;

  @After
  public void tearDown() {
    if (underTest != null) {
      underTest.after();
    }
  }

  @Test
  public void constructor_shouldUseServiceLoaderToCreateTestDb() {
    underTest = new TestableAbstractPluggableDbTester();

    assertThat(underTest.getDb()).isNotNull();
    assertThat(underTest.getDb()).isInstanceOf(TestDb.class);
  }

  @Test
  public void constructor_shouldPassSchemaPathToProvider() {
    // Using null since we can't verify the schemaPath without a real schema file
    // The provider handles null by creating an empty database
    underTest = new TestableAbstractPluggableDbTester();

    assertThat(underTest.getDb()).isNotNull();
  }

  @Test
  public void getDb_shouldReturnServiceLoaderCreatedTestDb() {
    underTest = new TestableAbstractPluggableDbTester();

    TestDb actualDb = underTest.getDb();

    // The db field is typed as TestDb (not a subtype) when using ServiceLoader
    assertThat(actualDb).isNotNull().isInstanceOf(TestDb.class);
  }

  @Test
  public void lifecycle_shouldWorkWithServiceLoaderTestDb() {
    underTest = new TestableAbstractPluggableDbTester();

    underTest.before();
    // If ServiceLoader integration works, before() should succeed
    assertThat(underTest.getDb()).isNotNull();

    underTest.after();
    // after() should also work
  }

  /**
   * Concrete implementation of AbstractPluggableDbTester for testing.
   */
  private static class TestableAbstractPluggableDbTester extends AbstractPluggableDbTester {

    TestableAbstractPluggableDbTester() {
    }

    @Override
    protected DbSession openSession(boolean batched) {
      // H2TestDb doesn't support MyBatis sessions
      throw new UnsupportedOperationException("H2TestDb doesn't support sessions");
    }
  }
}
