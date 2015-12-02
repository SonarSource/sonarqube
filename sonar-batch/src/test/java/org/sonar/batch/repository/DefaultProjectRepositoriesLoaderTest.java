/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.repository;

import com.google.common.io.Resources;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.cache.WSLoader;
import org.sonar.batch.cache.WSLoaderResult;
import org.sonarqube.ws.WsBatch.WsProjectResponse;
import org.sonarqube.ws.client.HttpException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultProjectRepositoriesLoaderTest {
  private final static String PROJECT_KEY = "foo?";
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DefaultProjectRepositoriesLoader loader;
  private WSLoader wsLoader;

  @Before
  public void prepare() throws IOException {
    wsLoader = mock(WSLoader.class);
    InputStream is = mockData();
    when(wsLoader.loadStream(anyString())).thenReturn(new WSLoaderResult<>(is, true));
    loader = new DefaultProjectRepositoriesLoader(wsLoader);
  }

  @Test
  public void continueOnError() {
    when(wsLoader.loadStream(anyString())).thenThrow(IllegalStateException.class);
    ProjectRepositories proj = loader.load(PROJECT_KEY, false, null);
    assertThat(proj.exists()).isEqualTo(false);
  }

  @Test
  public void parsingError() throws IOException {
    InputStream is = mock(InputStream.class);
    when(is.read()).thenThrow(IOException.class);

    when(wsLoader.loadStream(anyString())).thenReturn(new WSLoaderResult<>(is, false));
    loader.load(PROJECT_KEY, false, null);
  }

  @Test(expected = IllegalStateException.class)
  public void failFastHttpError() {
    HttpException http = new HttpException("url", 403);
    IllegalStateException e = new IllegalStateException("http error", http);
    when(wsLoader.loadStream(anyString())).thenThrow(e);
    loader.load(PROJECT_KEY, false, null);
  }
  
  @Test
  public void failFastHttpErrorMessageException() {
    thrown.expect(MessageException.class);
    thrown.expectMessage("http error");
    
    HttpException http = new HttpException("uri", 403);
    MessageException e = MessageException.of("http error", http);
    when(wsLoader.loadStream(anyString())).thenThrow(e);
    loader.load(PROJECT_KEY, false, null);
  }

  @Test
  public void passIssuesModeParameter() {
    loader.load(PROJECT_KEY, false, null);
    verify(wsLoader).loadStream("/batch/project.protobuf?key=foo%3F");

    loader.load(PROJECT_KEY, true, null);
    verify(wsLoader).loadStream("/batch/project.protobuf?key=foo%3F&issues_mode=true");
  }

  @Test
  public void deserializeResponse() throws IOException {
    MutableBoolean fromCache = new MutableBoolean();
    loader.load(PROJECT_KEY, false, fromCache);
    assertThat(fromCache.booleanValue()).isTrue();
  }

  @Test
  public void passAndEncodeProjectKeyParameter() {
    loader.load(PROJECT_KEY, false, null);
    verify(wsLoader).loadStream("/batch/project.protobuf?key=foo%3F");
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
    when(wsLoader.loadStream(anyString())).thenReturn(new WSLoaderResult<InputStream>(is, true));

    ProjectRepositories proj = loader.load("org.sonarsource.github:sonar-github-plugin", true, null);
    FileData fd = proj.fileData("org.sonarsource.github:sonar-github-plugin",
      "src/test/java/org/sonar/plugins/github/PullRequestIssuePostJobTest.java");

    assertThat(fd.revision()).isEqualTo("27bf2c54633d05c5df402bbe09471fe43bd9e2e5");
    assertThat(fd.hash()).isEqualTo("edb6b3b9ab92d8dc53ba90ab86cd422e");
  }

  private InputStream getTestResource(String name) throws IOException {
    return Resources.asByteSource(this.getClass().getResource(this.getClass().getSimpleName() + "/" + name))
      .openBufferedStream();
  }

}
