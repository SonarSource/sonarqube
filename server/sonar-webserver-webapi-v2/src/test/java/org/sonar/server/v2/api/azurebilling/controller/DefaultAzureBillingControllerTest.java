/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.v2.api.azurebilling.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.azurebilling.environment.AzureEnvironment;
import org.sonar.server.v2.api.azurebilling.response.AzureBillingRestResponse;
import org.sonar.server.v2.api.azurebilling.service.AzureBillingHandler;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultAzureBillingControllerTest {

  private DefaultAzureBillingController controller;
  private AzureBillingHandler mockHandler;
  private UserSession mockUserSession;
  private AzureEnvironment mockAzureEnvironment;

  @BeforeEach
  void setUp() {
    mockHandler = mock();
    mockUserSession = mock();
    mockAzureEnvironment = mock();
    controller = new DefaultAzureBillingController(mockHandler, mockUserSession, mockAzureEnvironment);
  }

  @Test
  void testBillAzureAccount_whenBillingEnabled_shouldReturnSuccessResponse() {
    when(mockAzureEnvironment.isAzureBillingEnabled()).thenReturn(true);

    ResponseEntity<AzureBillingRestResponse> mockResponse = ResponseEntity.ok().body(new AzureBillingRestResponse(true, null));
    when(mockHandler.billAzureAccount()).thenReturn(mockResponse);

    ResponseEntity<AzureBillingRestResponse> response = controller.billAzureAccount();

    verify(mockUserSession, times(1)).checkIsSystemAdministrator();
    verify(mockHandler, times(1)).billAzureAccount();
    assertTrue(response.getBody().success());
    assertNull(response.getBody().message());
  }

  @Test
  void testBillAzureAccount_whenBillingDisabled_shouldThrowException() {
    when(mockAzureEnvironment.isAzureBillingEnabled()).thenReturn(false);

    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      controller.billAzureAccount();
    });

    verify(mockUserSession, times(1)).checkIsSystemAdministrator();
    assertEquals("Azure billing is not enabled on this instance", exception.getMessage());
    verifyNoInteractions(mockHandler);
  }

  @Test
  void testBillAzureAccount_whenUserNotAuthorized_shouldThrowException() {
    when(mockAzureEnvironment.isAzureBillingEnabled()).thenReturn(true);

    doThrow(new SecurityException("Unauthorized")).when(mockUserSession).checkIsSystemAdministrator();

    SecurityException exception = assertThrows(SecurityException.class, () -> {
      controller.billAzureAccount();
    });

    verify(mockUserSession, times(1)).checkIsSystemAdministrator();
    verifyNoInteractions(mockHandler);
    assertEquals("Unauthorized", exception.getMessage());
  }
}
