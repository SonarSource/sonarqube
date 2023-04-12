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
package org.sonar.server.v2.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.server.platform.ws.LivenessChecker;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.common.ControllerIT;

import static org.mockito.Mockito.when;
import static org.sonar.server.user.SystemPasscodeImpl.PASSCODE_HTTP_HEADER;
import static org.sonar.server.v2.WebApiEndpoints.LIVENESS_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DefaultLivenessControllerIT extends ControllerIT {

  private static final String VALID_PASSCODE = "valid_passcode";
  private static final String INVALID_PASSCODE = "invalid_passcode";

  @Before
  public void setup() {
    super.setup();
    when(webAppContext.getBean(LivenessChecker.class).liveness()).thenReturn(true);
  }

  @After
  public void resetUsedMocks() {
    Mockito.reset(webAppContext.getBean(SystemPasscode.class));
    Mockito.reset(webAppContext.getBean(UserSession.class));
  }

  @Test
  public void getSystemLiveness_whenValidPasscode_shouldSucceed() throws Exception {
    when(webAppContext.getBean(SystemPasscode.class).isValidPasscode(VALID_PASSCODE)).thenReturn(true);

    mockMvc.perform(get(LIVENESS_ENDPOINT).header(PASSCODE_HTTP_HEADER, VALID_PASSCODE))
      .andExpect(status().isNoContent());
  }

  @Test
  public void getSystemLiveness_whenAdminCredential_shouldSucceed() throws Exception {
    when(webAppContext.getBean(UserSession.class).isSystemAdministrator()).thenReturn(true);

    mockMvc.perform(get(LIVENESS_ENDPOINT))
      .andExpect(status().isNoContent());
  }

  @Test
  public void getSystemLiveness_whenNoUserSessionAndNoPasscode_shouldReturnForbidden() throws Exception {
    mockMvc.perform(get(LIVENESS_ENDPOINT))
      .andExpectAll(
        status().isForbidden(),
        content().string("Insufficient privileges"));
  }

  @Test
  public void getSystemLiveness_whenInvalidPasscodeAndNoAdminCredentials_shouldReturnForbidden() throws Exception {
    when(webAppContext.getBean(SystemPasscode.class).isValidPasscode(INVALID_PASSCODE)).thenReturn(false);
    when(webAppContext.getBean(UserSession.class).isSystemAdministrator()).thenReturn(false);

    mockMvc.perform(get(LIVENESS_ENDPOINT).header(PASSCODE_HTTP_HEADER, INVALID_PASSCODE))
      .andExpectAll(
        status().isForbidden(),
        content().string("Insufficient privileges"));
  }

  @Test
  public void getSystemLiveness_whenLivenessCheckFails_shouldReturnServerError() throws Exception {
    when(webAppContext.getBean(SystemPasscode.class).isValidPasscode(VALID_PASSCODE)).thenReturn(true);
    when(webAppContext.getBean(LivenessChecker.class).liveness()).thenReturn(false);

    mockMvc.perform(get(LIVENESS_ENDPOINT).header(PASSCODE_HTTP_HEADER, VALID_PASSCODE))
      .andExpectAll(
        status().isInternalServerError(),
        content().string("Liveness check failed"));
  }
}
