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

import org.sonarqube.ws.WsScanner.WsProjectResponse;
import org.sonar.batch.cache.WSLoaderResult;
import org.sonar.batch.cache.WSLoader;
import org.apache.commons.lang.mutable.MutableBoolean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
    when(wsLoader.loadStream(anyString())).thenReturn(new WSLoaderResult<InputStream>(is, true));
    loader = new DefaultProjectRepositoriesLoader(wsLoader);
  }

  @Test
  public void continueOnError() {
    when(wsLoader.loadStream(anyString())).thenThrow(IllegalStateException.class);
    ProjectRepositories proj = loader.load(PROJECT_KEY, false, null);
    assertThat(proj.exists()).isEqualTo(false);
  }

  @Test
  public void passIssuesModeParameter() {
    loader.load(PROJECT_KEY, false, null);
    verify(wsLoader).loadStream("/batch/project?key=foo%3F");

    loader.load(PROJECT_KEY, true, null);
    verify(wsLoader).loadStream("/batch/project?key=foo%3F&issues=true");
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
    verify(wsLoader).loadStream("/batch/project?key=foo%3F");
  }

  private InputStream mockData() throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    WsProjectResponse.Builder projectResponseBuilder = WsProjectResponse.newBuilder();
    WsProjectResponse response = projectResponseBuilder.build();
    response.writeTo(os);

    return new ByteArrayInputStream(os.toByteArray());
  }

}
