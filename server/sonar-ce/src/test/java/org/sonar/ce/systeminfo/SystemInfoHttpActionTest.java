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
package org.sonar.ce.systeminfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.sonar.ce.httpd.CeHttpUtils;
import org.sonar.process.systeminfo.JvmStateSection;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemInfoHttpActionTest {
  private SystemInfoSection stateProvider1 = new JvmStateSection("state1");
  private SystemInfoSection stateProvider2 = new JvmStateSection("state2");
  private SystemInfoHttpAction underTest;

  @Before
  public void setUp() {
    underTest = new SystemInfoHttpAction(Arrays.asList(stateProvider1, stateProvider2));
  }

  @Test
  public void serves_METHOD_NOT_ALLOWED_error_when_method_is_not_GET() throws HttpException, IOException {
    CeHttpUtils.testHandlerForPostWithoutResponseBody(underTest, List.of(), List.of(), HttpStatus.SC_METHOD_NOT_ALLOWED);
  }

  @Test
  public void serves_data_from_SystemInfoSections() throws Exception {
    byte[] responsePayload = CeHttpUtils.testHandlerForGetWithResponseBody(underTest, HttpStatus.SC_OK);

    ProtobufSystemInfo.SystemInfo systemInfo = ProtobufSystemInfo.SystemInfo.parseFrom(responsePayload);
    assertThat(systemInfo.getSectionsCount()).isEqualTo(2);
    assertThat(systemInfo.getSections(0).getName()).isEqualTo("state1");
    assertThat(systemInfo.getSections(1).getName()).isEqualTo("state2");
  }
}
