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
package org.sonar.scanner.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import org.sonar.scanner.WsTestUtil;
import org.sonar.scanner.bootstrap.DefaultScannerWsClient;
import org.sonarqube.ws.NewCodePeriods;

import static org.mockito.Mockito.mock;

public class DefaultNewCodePeriodLoaderTest {

  private DefaultScannerWsClient wsClient = mock(DefaultScannerWsClient.class);
  private DefaultNewCodePeriodLoader underTest = new DefaultNewCodePeriodLoader(wsClient);

  @Test
  public void loads_new_code_period() throws IOException {
    prepareCallWithResults();
    underTest.load("project", "branch");
    verifyCalledPath("/api/new_code_periods/show.protobuf?project=project&branch=branch");
  }

  private void verifyCalledPath(String expectedPath) {
    WsTestUtil.verifyCall(wsClient, expectedPath);
  }

  private void prepareCallWithResults() throws IOException {
    WsTestUtil.mockStream(wsClient, createResponse(NewCodePeriods.NewCodePeriodType.REFERENCE_BRANCH, "main"));
  }

  private InputStream createResponse(NewCodePeriods.NewCodePeriodType type, String value) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    NewCodePeriods.ShowWSResponse response = NewCodePeriods.ShowWSResponse.newBuilder()
      .setType(type)
      .setValue(value)
      .build();

    response.writeTo(os);
    return new ByteArrayInputStream(os.toByteArray());
  }
}
