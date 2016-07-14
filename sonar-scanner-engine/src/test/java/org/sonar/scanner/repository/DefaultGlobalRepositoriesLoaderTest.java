/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.scanner.WsTestUtil;
import org.sonar.scanner.bootstrap.BatchWsClient;
import org.sonar.scanner.protocol.input.GlobalRepositories;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DefaultGlobalRepositoriesLoaderTest {
  private static final String BATCH_GLOBAL_URL = "/batch/global";
  private BatchWsClient wsClient;
  private DefaultGlobalRepositoriesLoader globalRepositoryLoader;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    wsClient = mock(BatchWsClient.class);
    WsTestUtil.mockReader(wsClient, BATCH_GLOBAL_URL, new StringReader(new GlobalRepositories().toJson()));
    globalRepositoryLoader = new DefaultGlobalRepositoriesLoader(wsClient);
  }

  @Test
  public void test() {
    globalRepositoryLoader.load();
    WsTestUtil.verifyCall(wsClient, BATCH_GLOBAL_URL);
    verifyNoMoreInteractions(wsClient);
  }

  @Test
  public void testIOError() throws IOException {
    Reader reader = mock(Reader.class);
    when(reader.read(any(char[].class))).thenThrow(new IOException());
    WsTestUtil.mockReader(wsClient, reader);
    exception.expect(IllegalStateException.class);
    globalRepositoryLoader.load();
  }

  @Test
  public void testCloseError() throws IOException {
    Reader reader = mock(Reader.class);
    when(reader.read(any(char[].class))).thenReturn(-1);
    doThrow(new IOException()).when(reader).close();
    WsTestUtil.mockReader(wsClient, reader);
    exception.expect(IllegalStateException.class);
    globalRepositoryLoader.load();
  }
}
