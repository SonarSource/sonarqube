/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.startup;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.Database;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClusterConfigurationCheckTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Database database = mock(Database.class);
  private ClusterConfigurationCheck underTest = new ClusterConfigurationCheck(database);

  @Test
  public void when_SQ_is_connected_to_MySql_an_ISE_should_be_thrown() {
    when(database.getDialect()).thenReturn(new MySql());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("MySQL is not supported for Data Center Edition. Please connect to a supported database: Oracle, PostgreSQL, Microsoft SQL Server.");

    underTest.start();
  }

  @Test
  public void when_SQ_is_connected_to_MsSql_an_ISE_should_NOT_be_thrown() {
    when(database.getDialect()).thenReturn(new MsSql());

    underTest.start();
  }

  @Test
  public void when_SQ_is_connected_to_Oracle_an_ISE_should_NOT_be_thrown() {
    when(database.getDialect()).thenReturn(new Oracle());

    underTest.start();
  }

  @Test
  public void when_SQ_is_connected_to_Postgres_an_ISE_should_NOT_be_thrown() {
    when(database.getDialect()).thenReturn(new Oracle());

    underTest.start();
  }
}
