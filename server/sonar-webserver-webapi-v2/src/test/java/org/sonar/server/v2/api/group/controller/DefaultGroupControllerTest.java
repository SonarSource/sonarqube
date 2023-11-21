/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.v2.api.group.controller;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.server.common.group.service.GroupService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.v2.WebApiEndpoints.GROUPS_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DefaultGroupControllerTest {

  private static final String GROUP_UUID = "1234";
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final GroupService groupService = mock(GroupService.class);
  private final DbClient dbClient = mock(DbClient.class);

  private final DbSession dbSession = mock(DbSession.class);

  private final MockMvc mockMvc = ControllerTester.getMockMvc(new DefaultGroupController(groupService, dbClient, userSession));

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
  }

  @Test
  public void fetchGroup_whenGroupExists_returnsTheGroup() throws Exception {

    GroupDto groupDto = new GroupDto().setUuid(GROUP_UUID).setName("name").setDescription("description");

    when(groupService.findGroupByUuid(dbSession, GROUP_UUID)).thenReturn(Optional.of(groupDto));

    userSession.logIn().setSystemAdministrator();
    mockMvc.perform(get(GROUPS_ENDPOINT + "/" + GROUP_UUID))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {
            "id": "1234",
            "name": "name",
            "description": "description"
          }
          """));
  }

  @Test
  public void fetchGroup_whenCallerIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();
    mockMvc.perform(
      get(GROUPS_ENDPOINT + "/" + GROUP_UUID))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void fetchGroup_whenGroupDoesntExist_shouldReturnNotFound() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(groupService.findGroupByUuid(dbSession, GROUP_UUID)).thenReturn(Optional.empty());
    mockMvc.perform(
      get(GROUPS_ENDPOINT + "/" + GROUP_UUID)
        .content("{}"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Group '1234' not found\"}"));
  }

}
