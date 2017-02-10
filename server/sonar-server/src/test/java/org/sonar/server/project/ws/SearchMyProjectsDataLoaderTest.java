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
package org.sonar.server.project.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.tester.UserSessionRule;
import org.sonarqube.ws.client.project.SearchMyProjectsRequest;

import static org.mockito.Mockito.mock;

public class SearchMyProjectsDataLoaderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  SearchMyProjectsDataLoader underTest = new SearchMyProjectsDataLoader(userSession, mock(DbClient.class));

  @Test
  public void NPE_when_user_is_not_authenticated() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Current user must be authenticated");

    userSession.anonymous();

    underTest.searchProjects(mock(DbSession.class), mock(SearchMyProjectsRequest.class));
  }
}
