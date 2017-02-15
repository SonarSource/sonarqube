/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.db.component;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceDaoTest {

  private static System2 system = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system);

  private ResourceDao underTest = dbTester.getDbClient().resourceDao();

  @Test
  public void update_authorization_date() {
    dbTester.prepareDbUnit(getClass(), "update_authorization_date.xml");

    when(system.now()).thenReturn(987654321L);
    underTest.updateAuthorizationDate(1L, dbTester.getSession());
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "update_authorization_date-result.xml", "projects");
  }
}
