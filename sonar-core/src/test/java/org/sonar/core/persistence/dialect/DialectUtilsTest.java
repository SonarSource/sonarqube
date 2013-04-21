/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.persistence.dialect;

import org.junit.Test;
import org.sonar.api.utils.SonarException;

import static org.fest.assertions.Assertions.assertThat;

public class DialectUtilsTest {

  @Test
  public void testFindById() {
    Dialect d = DialectUtils.find("mysql", null);
    assertThat(d).isInstanceOf(MySql.class);
  }

  @Test
  public void testFindByJdbcUrl() {
    Dialect d = DialectUtils.find(null, "jdbc:mysql:foo:bar");
    assertThat(d).isInstanceOf(MySql.class);
  }

  @Test(expected = SonarException.class)
  public void testFindNoMatch() {
    DialectUtils.find("foo", "bar");
  }
}
