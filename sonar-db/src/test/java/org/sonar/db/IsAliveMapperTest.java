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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class IsAliveMapperTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbSession session;
  IsAliveMapper underTest;

  @Before
  public void setUp() {
    session = dbTester.myBatis().openSession(false);
    underTest = session.getMapper(IsAliveMapper.class);
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void isAlive_works_for_current_vendors() {
    assertThat(underTest.isAlive()).isEqualTo(IsAliveMapper.IS_ALIVE_RETURNED_VALUE);
  }
}
