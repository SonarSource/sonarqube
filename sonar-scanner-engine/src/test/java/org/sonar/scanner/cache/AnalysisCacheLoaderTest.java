/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.scanner.cache;

import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.DeflaterInputStream;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.scanner.bootstrap.DefaultScannerWsClient;
import org.sonar.scanner.protocol.internal.ScannerInternal;
import org.sonar.scanner.protocol.internal.ScannerInternal.AnalysisCacheMsg;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonarqube.ws.client.WsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.scanner.cache.AnalysisCacheLoader.CONTENT_ENCODING;

public class AnalysisCacheLoaderTest {
  private final WsResponse response = mock(WsResponse.class);
  private final DefaultScannerWsClient wsClient = mock(DefaultScannerWsClient.class);
  private final InputProject project = mock(InputProject.class);
  private final BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
  private final AnalysisCacheLoader loader = new AnalysisCacheLoader(wsClient, project, branchConfiguration);

  @Before
  public void before() {
    when(wsClient.call(any())).thenReturn(response);
  }

  @Test
  public void loads_content() throws IOException {
    ScannerInternal.AnalysisCacheMsg expected = ScannerInternal.AnalysisCacheMsg.newBuilder()
      .putMap("key", ByteString.copyFrom("value", StandardCharsets.UTF_8))
      .build();
    setResponse(expected);
    AnalysisCacheMsg msg = loader.load().get();
    assertThat(msg).isEqualTo(expected);
  }

  @Test
  public void loads_compressed_content() throws IOException {
    AnalysisCacheMsg expected = AnalysisCacheMsg.newBuilder()
      .putMap("key", ByteString.copyFrom("value", StandardCharsets.UTF_8))
      .build();
    setCompressedResponse(expected);
    AnalysisCacheMsg msg = loader.load().get();
    assertThat(msg).isEqualTo(expected);
  }

  @Test
  public void returns_empty_if_404() {
    when(response.code()).thenReturn(404);
    assertThat(loader.load()).isEmpty();
  }

  private void setResponse(AnalysisCacheMsg msg) throws IOException {
    when(response.contentStream()).thenReturn(createInputStream(msg));
  }

  private void setCompressedResponse(AnalysisCacheMsg msg) throws IOException {
    when(response.contentStream()).thenReturn(new DeflaterInputStream(createInputStream(msg)));
    when(response.header(CONTENT_ENCODING)).thenReturn(Optional.of("gzip"));
  }

  private InputStream createInputStream(AnalysisCacheMsg analysisCacheMsg) throws IOException {
    ByteArrayOutputStream serialized = new ByteArrayOutputStream(analysisCacheMsg.getSerializedSize());
    analysisCacheMsg.writeTo(serialized);
    return new ByteArrayInputStream(serialized.toByteArray());
  }
}
