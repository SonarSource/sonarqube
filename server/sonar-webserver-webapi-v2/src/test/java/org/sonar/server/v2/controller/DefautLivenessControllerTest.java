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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.server.platform.ws.LivenessChecker;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.common.RestResponseEntityExceptionHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.sonar.server.user.SystemPasscodeImpl.PASSCODE_HTTP_HEADER;
import static org.sonar.server.v2.WebApiEndpoints.LIVENESS_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class DefautLivenessControllerTest {

  private static final String PASSCODE = "1234";
  @Mock
  private LivenessChecker livenessChecker;
  @Mock
  private UserSession userSession;
  @Mock
  private SystemPasscode systemPasscode;
  @InjectMocks
  private DefautLivenessController defautLivenessController;

  private MockMvc mockMvc;

  @Before
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(defautLivenessController)
      .setControllerAdvice(RestResponseEntityExceptionHandler.class)
      .build();
  }

  @Test
  public void livenessCheck_inSafeModeWithoutUserSessionAndPasscode_returnsForbidden() throws Exception {
    LivenessController safeModeLivenessController = new DefautLivenessController(livenessChecker, systemPasscode, null);
    MockMvc mockMvcSafeMode = MockMvcBuilders.standaloneSetup(safeModeLivenessController)
      .setControllerAdvice(RestResponseEntityExceptionHandler.class)
      .build();
    mockMvcSafeMode.perform(get(LIVENESS_ENDPOINT))
      .andExpect(status().isForbidden());
  }

  @Test
  public void livenessCheck_should_returnForbiddenWithNoCredentials() throws Exception {
    mockMvc.perform(get(LIVENESS_ENDPOINT))
      .andExpect(status().isForbidden());
  }

  @Test
  public void livenessCheck_should_returnForbiddenWithWrongPasscodeAndNoAdminCredentials() throws Exception {
    when(systemPasscode.isValidPasscode(PASSCODE)).thenReturn(false);
    when(userSession.isSystemAdministrator()).thenReturn(false);
    mockMvc.perform(get(LIVENESS_ENDPOINT).header(PASSCODE_HTTP_HEADER, PASSCODE))
      .andExpect(status().isForbidden());
  }

  @Test
  public void livenessCheck_should_returnNoContentWithSystemPasscode() throws Exception {
    when(systemPasscode.isValidPasscode(PASSCODE)).thenReturn(true);
    when(livenessChecker.liveness()).thenReturn(true);
    mockMvc.perform(get(LIVENESS_ENDPOINT).header(PASSCODE_HTTP_HEADER, PASSCODE))
      .andExpect(status().isNoContent());
  }

  @Test
  public void livenessCheck_should_returnNoContentWithWhenUserIsAdmin() throws Exception {
    when(userSession.isSystemAdministrator()).thenReturn(true);
    when(livenessChecker.liveness()).thenReturn(true);
    mockMvc.perform(get(LIVENESS_ENDPOINT).header(PASSCODE_HTTP_HEADER, PASSCODE))
      .andExpect(status().isNoContent());
  }

  @Test
  public void livenessCheck_should_returnServerErrorWhenLivenessCheckFails() throws Exception {
    when(systemPasscode.isValidPasscode(PASSCODE)).thenReturn(true);
    when(livenessChecker.liveness()).thenReturn(false);
    mockMvc.perform(get(LIVENESS_ENDPOINT).header(PASSCODE_HTTP_HEADER, PASSCODE))
      .andExpect(status().isInternalServerError());
  }

}
