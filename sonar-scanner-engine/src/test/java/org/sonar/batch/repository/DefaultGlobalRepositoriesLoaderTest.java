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
package org.sonar.batch.repository;

import org.junit.Before;
import org.sonar.batch.WsTestUtil;
import org.sonar.batch.bootstrap.BatchWsClient;
import org.sonar.scanner.protocol.input.GlobalRepositories;

import java.io.StringReader;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DefaultGlobalRepositoriesLoaderTest {
  private static final String BATCH_GLOBAL_URL = "/batch/global";
  private BatchWsClient wsClient;
  private DefaultGlobalRepositoriesLoader globalRepositoryLoader;

  @Before
  public void setUp() {
    wsClient = mock(BatchWsClient.class);
    WsTestUtil.mockReader(wsClient, BATCH_GLOBAL_URL, new StringReader(new GlobalRepositories().toJson()));
    globalRepositoryLoader = new DefaultGlobalRepositoriesLoader(wsClient);
  }

  public void test() {
    globalRepositoryLoader.load();
    WsTestUtil.verifyCall(wsClient, BATCH_GLOBAL_URL);
    verifyNoMoreInteractions(wsClient);
  }
}
