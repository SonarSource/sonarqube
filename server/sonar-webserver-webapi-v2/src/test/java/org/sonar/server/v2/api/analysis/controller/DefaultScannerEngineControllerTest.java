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
package org.sonar.server.v2.api.analysis.controller;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.server.v2.api.analysis.service.ScannerEngineHandler;
import org.sonar.server.v2.api.analysis.service.ScannerEngineMetadata;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static java.lang.String.format;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.write;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.v2.WebApiEndpoints.SCANNER_ENGINE_ENDPOINT;
import static org.sonar.server.v2.api.ControllerTester.getMockMvc;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultScannerEngineControllerTest {

  private final ScannerEngineHandler scannerEngineHandler = mock(ScannerEngineHandler.class);

  private final MockMvc mockMvc = getMockMvc(new DefaultScannerEngineController(scannerEngineHandler));

  @Test
  void getEngine_shouldReturnScannerMetadataAsJson() throws Exception {
    String anyName = "anyName";
    String anyChecksum = "anyChecksum";
    when(scannerEngineHandler.getScannerEngineMetadata()).thenReturn(new ScannerEngineMetadata(anyName, anyChecksum));
    String expectedJson = format("{\"filename\":\"%s\",\"checksum\":\"%s\"}", anyName, anyChecksum);

    mockMvc.perform(get(SCANNER_ENGINE_ENDPOINT))
      .andExpectAll(
        status().isOk(),
        content().json(expectedJson));
  }

  @Test
  void getEngine_shouldDownloadScanner_whenHeaderIsOctetStream(@TempDir Path tempDir) throws Exception {
    File scanner = createTempFile(tempDir, "scanner", ".jar").toFile();
    byte[] anyBinary = {1, 2, 3};
    write(scanner.toPath(), anyBinary);
    when(scannerEngineHandler.getScannerEngine()).thenReturn(new File(scanner.toString()));

    mockMvc.perform(get(SCANNER_ENGINE_ENDPOINT)
        .header("Accept", APPLICATION_OCTET_STREAM_VALUE))
      .andExpectAll(
        status().isOk(),
        content().contentType(APPLICATION_OCTET_STREAM),
        content().bytes(anyBinary));
  }

  @Test
  void getEngine_shouldFail_whenScannerEngineNotFound() {
    // Ideally we would like Spring to return a 404, but it's not the case at the moment. We suspect that it's because the Header Accept wants a binary file.
    // So the Json corresponding to the NotFoundException is not sent and we have a 500 instead.
    when(scannerEngineHandler.getScannerEngine()).thenReturn(new File("no-file"));
    MockHttpServletRequestBuilder request = get(SCANNER_ENGINE_ENDPOINT).header("Accept", APPLICATION_OCTET_STREAM_VALUE);
    assertThatThrownBy(() -> mockMvc.perform(request))
      .hasMessageContaining("NotFoundException: Unable to find file: no-file");
  }

}
