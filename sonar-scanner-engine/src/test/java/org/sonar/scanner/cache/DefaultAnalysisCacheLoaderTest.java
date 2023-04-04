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
package org.sonar.scanner.cache;

import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.api.utils.MessageException;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.scanner.bootstrap.DefaultScannerWsClient;
import org.sonar.scanner.protocol.internal.ScannerInternal.SensorCacheEntry;
import org.sonar.scanner.protocol.internal.SensorCacheData;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsRequest;
import org.sonarqube.ws.client.WsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.scanner.cache.DefaultAnalysisCacheLoader.CONTENT_ENCODING;

public class DefaultAnalysisCacheLoaderTest {
  private final static SensorCacheEntry MSG = SensorCacheEntry.newBuilder()
    .setKey("key")
    .setData(ByteString.copyFrom("value", StandardCharsets.UTF_8))
    .build();
  private final WsResponse response = mock(WsResponse.class);
  private final DefaultScannerWsClient wsClient = mock(DefaultScannerWsClient.class);
  private final InputProject project = mock(InputProject.class);
  private final BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
  private final DefaultAnalysisCacheLoader loader = new DefaultAnalysisCacheLoader(wsClient, project, branchConfiguration);
  @Rule
  public LogTester logs = new LogTester();

  @Before
  public void before() {
    when(project.key()).thenReturn("myproject");
    when(wsClient.call(any())).thenReturn(response);
  }

  @Test
  public void loads_content_and_logs_size() throws IOException {
    setResponse(MSG);
    when(response.header("Content-Length")).thenReturn(Optional.of("123"));
    SensorCacheData msg = loader.load().get();
    assertThat(msg.getEntries()).containsOnly(entry(MSG.getKey(), MSG.getData()));
    assertRequestPath("api/analysis_cache/get?project=myproject");
    assertThat(logs.logs()).anyMatch(s -> s.startsWith("Load analysis cache (123 bytes)"));
  }

  @Test
  public void loads_content_for_branch() throws IOException {
    when(branchConfiguration.referenceBranchName()).thenReturn("name");

    setResponse(MSG);
    SensorCacheData msg = loader.load().get();

    assertThat(msg.getEntries()).containsOnly(entry(MSG.getKey(), MSG.getData()));
    assertRequestPath("api/analysis_cache/get?project=myproject&branch=name");
    assertThat(logs.logs()).anyMatch(s -> s.startsWith("Load analysis cache | time="));
  }

  @Test
  public void loads_compressed_content() throws IOException {
    setCompressedResponse(MSG);
    SensorCacheData msg = loader.load().get();
    assertThat(msg.getEntries()).containsOnly(entry(MSG.getKey(), MSG.getData()));
  }

  @Test
  public void returns_empty_if_404() {
    when(wsClient.call(any())).thenThrow(new HttpException("url", 404, "content"));
    assertThat(loader.load()).isEmpty();
    assertThat(logs.logs()).anyMatch(s -> s.startsWith("Load analysis cache (404) | time="));
  }

  @Test
  public void throw_error_if_http_exception_not_404() {
    when(wsClient.call(any())).thenThrow(new HttpException("url", 401, "content"));
    assertThatThrownBy(loader::load)
      .isInstanceOf(MessageException.class)
      .hasMessage("Failed to download analysis cache: HTTP code 401: content");
  }

  @Test
  public void throw_error_if_cant_decompress_content() {
    setInvalidCompressedResponse();
    assertThatThrownBy(loader::load)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to download analysis cache");
  }

  private void assertRequestPath(String expectedPath) {
    ArgumentCaptor<WsRequest> requestCaptor = ArgumentCaptor.forClass(WsRequest.class);
    verify(wsClient).call(requestCaptor.capture());
    assertThat(requestCaptor.getValue().getPath()).isEqualTo(expectedPath);
  }

  private void setResponse(SensorCacheEntry msg) throws IOException {
    when(response.contentStream()).thenReturn(createInputStream(msg));
  }

  private void setCompressedResponse(SensorCacheEntry msg) throws IOException {
    when(response.contentStream()).thenReturn(createCompressedInputStream(msg));
    when(response.header(CONTENT_ENCODING)).thenReturn(Optional.of("gzip"));
  }

  private void setInvalidCompressedResponse() {
    when(response.contentStream()).thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));
    when(response.header(CONTENT_ENCODING)).thenReturn(Optional.of("gzip"));
  }

  private InputStream createInputStream(SensorCacheEntry analysisCacheMsg) throws IOException {
    ByteArrayOutputStream serialized = new ByteArrayOutputStream(analysisCacheMsg.getSerializedSize());
    analysisCacheMsg.writeDelimitedTo(serialized);
    return new ByteArrayInputStream(serialized.toByteArray());
  }

  private InputStream createCompressedInputStream(SensorCacheEntry analysisCacheMsg) throws IOException {
    ByteArrayOutputStream serialized = new ByteArrayOutputStream(analysisCacheMsg.getSerializedSize());
    GZIPOutputStream compressed = new GZIPOutputStream(serialized);
    analysisCacheMsg.writeDelimitedTo(compressed);
    compressed.close();
    return new ByteArrayInputStream(serialized.toByteArray());
  }
}
