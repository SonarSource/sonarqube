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
package org.sonar.server.v2.api.analysis.controller;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.server.v2.api.analysis.response.JreInfoRestResponse;
import org.sonar.server.v2.api.analysis.service.JresHandler;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.v2.WebApiEndpoints.JRE_ENDPOINT;
import static org.sonar.server.v2.api.ControllerTester.getMockMvc;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultJresControllerTest {

  private final JresHandler jresHandler = mock(JresHandler.class);

  private final MockMvc mockMvc = getMockMvc(new DefaultJresController(jresHandler));

  @ParameterizedTest
  @MethodSource("osAndArch")
  void getJres_shoudlReturnMetadaAsJson(String os, String arch) throws Exception {
    JreInfoRestResponse metadata1 = new JreInfoRestResponse("id_1", "filename_1", "sha256_1", "javaPath_1", "os_1", "arch_1");
    JreInfoRestResponse metadata2 = new JreInfoRestResponse("id_2", "filename_2", "sha256_2", "javaPath_2", "os_2", "arch_2");
    when(jresHandler.getJresMetadata(os, arch)).thenReturn(List.of(metadata1, metadata2));
    String expectedJson =
      """
          [
            {
              "id": "id_1",
              "filename": "filename_1",
              "sha256": "sha256_1",
              "javaPath": "javaPath_1",
              "os": "os_1",
              "arch": "arch_1"
            },
            {
              "id": "id_2",
              "filename": "filename_2",
              "sha256": "sha256_2",
              "javaPath": "javaPath_2",
              "os": "os_2",
              "arch": "arch_2"
            }
          ]
        """;

    mockMvc.perform(get(JRE_ENDPOINT)
        .param("os", os)
        .param("arch", arch))
      .andExpect(status().isOk())
      .andExpect(content().json(expectedJson));
  }

  @Test
  void getJres_shoudlReturnEmptyJsonArray_whenNoResults() throws Exception {
    when(jresHandler.getJresMetadata(null, null)).thenReturn(List.of());

    mockMvc.perform(get(JRE_ENDPOINT))
      .andExpect(status().isOk())
      .andExpect(content().json("[]"));
  }

  private static Stream<Arguments> osAndArch() {
    return Stream.of(
      arguments(null, null),
      arguments("windows", null),
      arguments(null, "x64"),
      arguments("linux", "aarch64")
    );
  }

  @Test
  void getJre_shouldReturnMetadataAsJson() throws Exception {
    String anyId = "anyId";
    JreInfoRestResponse jreInfoRestResponse = new JreInfoRestResponse(anyId, "filename", "sha256", "javaPath", "os", "arch");
    when(jresHandler.getJreMetadata(anyId)).thenReturn(jreInfoRestResponse);
    String expectedJson = "{\"id\":\"" + anyId + "\",\"filename\":\"filename\",\"sha256\":\"sha256\",\"javaPath\":\"javaPath\",\"os\":\"os\",\"arch\":\"arch\"}";

    mockMvc.perform(get(JRE_ENDPOINT + "/" + anyId))
      .andExpect(status().isOk())
      .andExpect(content().json(expectedJson));
  }

  @Test
  void getJre_shouldDownloadJre_whenHeaderIsOctetStream() throws Exception {
    String anyId = "anyId";
    String anyFilename = "anyFilename";
    JreInfoRestResponse jreInfoRestResponse = new JreInfoRestResponse(anyId, anyFilename, "sha256", "javaPath", "os", "arch");
    when(jresHandler.getJreMetadata(anyId)).thenReturn(jreInfoRestResponse);
    byte[] anyBinary = {1, 2, 3};

    when(jresHandler.getJreBinary(anyFilename)).thenReturn(new ByteArrayInputStream(anyBinary));

    mockMvc.perform(get(JRE_ENDPOINT + "/" + anyId)
        .header("Accept", APPLICATION_OCTET_STREAM_VALUE))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_OCTET_STREAM))
      .andExpect(content().bytes(anyBinary));
  }

}
