/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.v2.api.dop.controller;

import com.google.gson.Gson;
import java.util.List;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.dop.response.DopSettingsResource;
import org.sonar.server.v2.api.dop.response.DopSettingsRestResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.v2.WebApiEndpoints.DOP_SETTINGS_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultDopSettingsControllerTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final DbClient dbClient = mock(DbClient.class, RETURNS_DEEP_STUBS);
  private final DbSession dbSession = mock();

  private final MockMvc mockMvc = ControllerTester.getMockMvc(
    new DefaultDopSettingsController(userSession, dbClient));

  private static final Gson gson = new Gson();

  @BeforeEach
  void setup() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
  }

  @Test
  void fetchAllDopSettings_whenUserDoesntHaveCreateProjectPermission_returnsForbidden() throws Exception {
    userSession.logIn();
    mockMvc
      .perform(get(DOP_SETTINGS_ENDPOINT))
      .andExpect(status().isForbidden());
  }

  @Test
  void fetchAllDopSettings_whenDbClientReturnsData_returnsResponse() throws Exception {
    List<AlmSettingDto> dopSettings = List.of(
      generateAlmSettingsDto("github"),
      generateAlmSettingsDto("azure"),
      generateAlmSettingsDto("bitbucket_cloud")
    );
    when(dbClient.almSettingDao().selectAll(dbSession)).thenReturn(dopSettings);

    userSession.logIn().addPermission(PROVISION_PROJECTS);;
    MvcResult mvcResult = mockMvc
      .perform(get(DOP_SETTINGS_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();
    DopSettingsRestResponse response = gson.fromJson(mvcResult.getResponse().getContentAsString(), DopSettingsRestResponse.class);
    assertThat(response.dopSettings())
      .containsExactlyInAnyOrderElementsOf(dopSettings.stream().map(DefaultDopSettingsControllerTest::toDopSettingsResource).toList());
  }

  private static DopSettingsResource toDopSettingsResource(AlmSettingDto almSettingDto) {
    return new DopSettingsResource(
      almSettingDto.getUuid(),
      almSettingDto.getRawAlm(),
      almSettingDto.getKey(),
      almSettingDto.getUrl(),
      almSettingDto.getAppId()
    );
  }

  private AlmSettingDto generateAlmSettingsDto(String dopType) {
    AlmSettingDto dto = mock();
    when(dto.getUuid()).thenReturn("uuid_" + dopType);
    when(dto.getRawAlm()).thenReturn(dopType);
    when(dto.getKey()).thenReturn("key_" + dopType);
    when(dto.getUrl()).thenReturn("url_" + dopType);
    when(dto.getAppId()).thenReturn("appId_" + dopType);
    return dto;
  }

}
