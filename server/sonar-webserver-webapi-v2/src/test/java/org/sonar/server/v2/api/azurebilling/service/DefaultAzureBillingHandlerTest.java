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
package org.sonar.server.v2.api.azurebilling.service;

import java.io.IOException;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.db.DbClient;
import org.sonar.db.project.ProjectDao;
import org.sonar.server.v2.api.azurebilling.response.AzureBillingRestResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAzureBillingHandlerTest {

  private final DbClient dbClient = mock();
  private final ProjectDao projectDao = mock();
  private final OkHttpClient mockHttpClient = mock();
  private final Call mockCall = mock();
  private final Response mockResponse = mock();
  private final ResponseBody mockResponseBody = mock();
  private final DefaultAzureBillingHandler underTest = new DefaultAzureBillingHandler(dbClient, mockHttpClient);

  @BeforeEach
  void setUp() throws IOException {
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(projectDao.getNclocSum(any())).thenReturn(1000L);
    when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
    when(mockCall.execute()).thenReturn(mockResponse);
  }

  @Test
  void testBillAzureAccount_whenCallIsOkay_thenMessageShouldBeNull() throws IOException {
    when(mockResponse.isSuccessful()).thenReturn(true);
    when(mockResponse.body()).thenReturn(mockResponseBody);
    when(mockResponseBody.string()).thenReturn("Success");

    AzureBillingRestResponse response = underTest.billAzureAccount("test-token");

    assertTrue(response.success());
    assertNull(response.message());
    verify(mockHttpClient, times(1)).newCall(any(Request.class));
    verify(mockCall, times(1)).execute();
  }

  @Test
  void testBillAzureAccount_whenCallIsNotOkay_thenMessageShouldNotBeNull() throws IOException {
    when(mockResponse.isSuccessful()).thenReturn(false);
    when(mockResponse.message()).thenReturn("Bad Request");

    AzureBillingRestResponse response = underTest.billAzureAccount("test-token");

    assertFalse(response.success());
    assertEquals("Call to Azure marketplace failed. Details: Bad Request", response.message());
    verify(mockHttpClient, times(1)).newCall(any(Request.class));
    verify(mockCall, times(1)).execute();
  }

  @Test
  void testBillAzureAccount_whenNetworkError_thenSuccessShouldBeFalse() throws IOException {
    when(mockCall.execute()).thenThrow(new IOException("Network error"));

    AzureBillingRestResponse response = underTest.billAzureAccount("test-token");

    assertFalse(response.success());
    assertEquals("Connection to Azure marketplace failed. Details: Network error", response.message());
    verify(mockHttpClient, times(1)).newCall(any(Request.class));
    verify(mockCall, times(1)).execute();
  }

  @Test
  void testBillAzureAccount_whenLocCannotBeObtained_thenSuccessShouldBeFalse() throws IOException {
    when(projectDao.getNclocSum(any())).thenThrow(new IllegalStateException("Database error"));

    AzureBillingRestResponse response = underTest.billAzureAccount("test-token");

    assertFalse(response.success());
    assertEquals("Failed to build request. Details: Database error", response.message());
    verify(mockHttpClient, times(0)).newCall(any(Request.class));
    verify(mockCall, times(0)).execute();
  }

}
