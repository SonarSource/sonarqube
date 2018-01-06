/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v60;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.db.Database;
import org.sonar.db.dialect.MySql;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DropIndexProjectsUuidFromProjectsTest {

  private Database db = mock(Database.class, Mockito.RETURNS_DEEP_STUBS);
  private DdlChange.Context context = mock(DdlChange.Context.class);

  @Before
  public void setUp() {
    // Some databases have unique names of indexes, so table name is not declared
    // when dropping an index ("drop index <index name>"). Because of that MySQL is
    // used in the test so that the table name can also be verified
    when(db.getDialect()).thenReturn(new MySql());
  }

  @Test
  public void drop_index() throws Exception {
    DropIndexProjectsUuidFromProjects underTest = new DropIndexProjectsUuidFromProjects(db);

    underTest.execute(context);

    verify(context).execute(asList("DROP INDEX projects_uuid ON projects"));
    verifyNoMoreInteractions(context);
  }

}
