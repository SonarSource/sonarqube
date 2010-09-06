/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.jpa.dialect;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class MsSqlTest {

  @Test
  public void matchesJdbcURL() {
    assertThat(new MsSql().matchesJdbcURL("jdbc:jtds:sqlserver://localhost;databaseName=SONAR;SelectMethod=Cursor"), is(true));
    assertThat(new MsSql().matchesJdbcURL("jdbc:microsoft:sqlserver://localhost:1433;databasename=sonar"), is(true));

    assertThat(new MsSql().matchesJdbcURL("jdbc:hsql:foo"), is(false));
    assertThat(new MsSql().matchesJdbcURL("jdbc:mysql:foo"), is(false));
  }

}
