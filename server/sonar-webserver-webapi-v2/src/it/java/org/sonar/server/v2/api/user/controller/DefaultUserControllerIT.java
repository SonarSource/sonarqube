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
package org.sonar.server.v2.api.user.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.db.DbClient;
import org.sonar.db.user.UserDao;
import org.sonar.server.common.user.service.UserService;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.AbstractUserSession;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.common.ControllerIT;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.v2.WebApiEndpoints.USER_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DefaultUserControllerIT extends ControllerIT {

  @After
  public void resetUsedMocks() {
    Mockito.reset(webAppContext.getBean(UserService.class));
    Mockito.reset(webAppContext.getBean(UserSession.class));
  }

  @Before
  public void setUp() {
    UserSession userSession = webAppContext.getBean(UserSession.class);
    when(userSession.checkLoggedIn()).thenReturn(userSession);
  }

  @Test
  public void deactivate_whenUserIsNotLoggedIn_shouldReturnForbidden() throws Exception {
    when(webAppContext.getBean(UserSession.class).checkLoggedIn()).thenThrow(new UnauthorizedException("unauthorized"));
    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete"))
      .andExpectAll(
        status().isUnauthorized(),
        content().string("{\"message\":\"unauthorized\"}"));
  }

  @Test
  public void deactivate_whenUserIsNotAdministrator_shouldReturnForbidden() throws Exception {
    when(webAppContext.getBean(UserSession.class).checkIsSystemAdministrator()).thenThrow(AbstractUserSession.insufficientPrivilegesException());
    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete"))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void deactivate_whenUserServiceThrowsNotFoundException_shouldReturnNotFound() throws Exception {
    doThrow(new NotFoundException("User not found.")).when(webAppContext.getBean(UserService.class)).deactivate("userToDelete", false);
    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"User not found.\"}"));
  }

  @Test
  public void deactivate_whenUserServiceThrowsBadRequestException_shouldReturnBadRequest() throws Exception {
    doThrow(BadRequestException.create("Not allowed")).when(webAppContext.getBean(UserService.class)).deactivate("userToDelete", false);
    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete"))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"Not allowed\"}"));
  }

  @Test
  public void deactivate_whenUserTryingToDeactivateThemself_shouldReturnBadRequest() throws Exception {
    when(webAppContext.getBean(DbClient.class).userDao()).thenReturn(mock(UserDao.class));
    when(webAppContext.getBean(UserSession.class).getLogin()).thenReturn("userToDelete");
    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete"))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"Self-deactivation is not possible\"}"));
  }

  @Test
  public void deactivate_whenAnonymizeParameterIsNotBoolean_shouldReturnBadRequest() throws Exception {
    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete?anonymize=maybe"))
      .andExpect(
        status().isBadRequest());
  }

  @Test
  public void deactivate_whenAnonymizeIsNotSpecified_shouldDeactivateUserWithoutAnonymization() throws Exception {
    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete"))
      .andExpect(status().isNoContent());

    verify(webAppContext.getBean(UserService.class)).deactivate("userToDelete", false);
  }

  @Test
  public void deactivate_whenAnonymizeFalse_shouldDeactivateUserWithoutAnonymization() throws Exception {
    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete?anonymize=false"))
      .andExpect(status().isNoContent());

    verify(webAppContext.getBean(UserService.class)).deactivate("userToDelete", false);
  }

  @Test
  public void deactivate_whenAnonymizeTrue_shouldDeactivateUserWithAnonymization() throws Exception {
    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete?anonymize=true"))
      .andExpect(status().isNoContent());

    verify(webAppContext.getBean(UserService.class)).deactivate("userToDelete", true);
  }
}
