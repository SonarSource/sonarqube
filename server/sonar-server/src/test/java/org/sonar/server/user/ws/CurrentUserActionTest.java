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
package org.sonar.server.user.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

public class CurrentUserActionTest {

  private WsTester tester;

  @Before
  public void before() {
    tester = new WsTester(new UsersWs(new CurrentUserAction()));
  }

  @Test
  public void anonymous() throws Exception {
    MockUserSession.set();
    tester.newGetRequest("api/users", "current").execute().assertJson(getClass(), "anonymous.json");
  }

  @Test
  public void authenticated() throws Exception {
    MockUserSession.set().setLogin("obiwan.kenobi").setName("Obiwan Kenobi")
      .setGlobalPermissions(GlobalPermissions.ALL.toArray(new String[0]));
    tester.newGetRequest("api/users", "current").execute().assertJson(getClass(), "authenticated.json");
  }
}
