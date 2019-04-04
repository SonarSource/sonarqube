/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.io.Resources;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.WsTestUtil;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonarqube.ws.Batch.WsProjectResponse;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultProjectRepositoriesLoaderTest {
  private final static String PROJECT_KEY = "foo?";
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DefaultProjectRepositoriesLoader loader;
  private ScannerWsClient wsClient;

  @Before
  public void prepare() throws IOException {
    wsClient = mock(ScannerWsClient.class);
    InputStream is = mockData();
    WsTestUtil.mockStream(wsClient, "/batch/project.protobuf?key=foo%3F", is);
    loader = new DefaultProjectRepositoriesLoader(wsClient);
  }

  @Test
  public void continueOnHttp404Exception() {
    when(wsClient.call(any(WsRequest.class))).thenThrow(new HttpException("/batch/project.protobuf?key=foo%3F", HttpURLConnection.HTTP_NOT_FOUND, ""));
    ProjectRepositories proj = loader.load(PROJECT_KEY, null);
    assertThat(proj.exists()).isEqualTo(false);
  }

  @Test(expected = IllegalStateException.class)
  public void failOnNonHttp404Exception() {
    when(wsClient.call(any(WsRequest.class))).thenThrow(IllegalStateException.class);
    ProjectRepositories proj = loader.load(PROJECT_KEY, null);
    assertThat(proj.exists()).isEqualTo(false);
  }

  @Test(expected = IllegalStateException.class)
  public void parsingError() throws IOException {
    InputStream is = mock(InputStream.class);
    when(is.read(any(byte[].class), anyInt(), anyInt())).thenThrow(IOException.class);
    WsTestUtil.mockStream(wsClient, "/batch/project.protobuf?key=foo%3F", is);
    loader.load(PROJECT_KEY, null);
  }

  @Test(expected = IllegalStateException.class)
  public void failFastHttpError() {
    HttpException http = new HttpException("url", 403, null);
    IllegalStateException e = new IllegalStateException("http error", http);
    WsTestUtil.mockException(wsClient, e);
    loader.load(PROJECT_KEY, null);
  }

  @Test
  public void failFastHttpErrorMessageException() {
    thrown.expect(MessageException.class);
    thrown.expectMessage("http error");

    HttpException http = new HttpException("uri", 403, null);
    MessageException e = MessageException.of("http error", http);
    WsTestUtil.mockException(wsClient, e);
    loader.load(PROJECT_KEY, null);
  }

  @Test
  public void deserializeResponse() throws IOException {
    loader.load(PROJECT_KEY, null);
  }

  @Test
  public void passAndEncodeProjectKeyParameter() {
    loader.load(PROJECT_KEY, null);
    WsTestUtil.verifyCall(wsClient, "/batch/project.protobuf?key=foo%3F");
  }

  private InputStream mockData() throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    WsProjectResponse.Builder projectResponseBuilder = WsProjectResponse.newBuilder();
    WsProjectResponse response = projectResponseBuilder.build();
    response.writeTo(os);

    return new ByteArrayInputStream(os.toByteArray());
  }

  @Test
  public void readRealResponse() throws IOException {
    InputStream is = getTestResource("project.protobuf");
    WsTestUtil.mockStream(wsClient, "/batch/project.protobuf?key=org.sonarsource.github%3Asonar-github-plugin", is);

    DefaultInputFile file = mock(DefaultInputFile.class);
    when(file.getModuleRelativePath()).thenReturn("src/test/java/org/sonar/plugins/github/PullRequestIssuePostJobTest.java");

    ProjectRepositories proj = loader.load("org.sonarsource.github:sonar-github-plugin", null);
    FileData fd = proj.fileData("org.sonarsource.github:sonar-github-plugin", file);

    assertThat(fd.revision()).isEqualTo("27bf2c54633d05c5df402bbe09471fe43bd9e2e5");
    assertThat(fd.hash()).isEqualTo("edb6b3b9ab92d8dc53ba90ab86cd422e");
  }

  private InputStream getTestResource(String name) throws IOException {
    return Resources.asByteSource(this.getClass().getResource(this.getClass().getSimpleName() + "/" + name))
      .openBufferedStream();
  }

}
