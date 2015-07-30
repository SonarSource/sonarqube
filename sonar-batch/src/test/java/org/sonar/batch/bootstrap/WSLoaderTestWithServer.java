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
package org.sonar.batch.bootstrap;

import static org.mockito.Mockito.mock;
import org.sonar.batch.bootstrap.WSLoader.LoadStrategy;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.home.cache.PersistentCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WSLoaderTestWithServer {
  private static final String RESPONSE_STRING = "this is the content";
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private MockHttpServer server;
  private PersistentCache cache;
  private ServerClient client;
  private WSLoader loader;

  @Before
  public void setUp() throws Exception {
    server = new MockHttpServer();
    server.start();

    BootstrapProperties bootstrapProps = mock(BootstrapProperties.class);
    when(bootstrapProps.property("sonar.host.url")).thenReturn("http://localhost:" + server.getPort());

    client = new ServerClient(bootstrapProps, new EnvironmentInformation("Junit", "4"));
    cache = new PersistentCache(temp.getRoot().toPath(), 1000 * 60, new Slf4jLogger(), null);
    loader = new WSLoader(cache, client);
  }

  @After
  public void tearDown() {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void testServer() {
    loader.setCacheEnabled(false);
    loader.setStrategy(LoadStrategy.SERVER_FIRST);
    server.setMockResponseData(RESPONSE_STRING);
    assertThat(loader.loadString("/foo")).isEqualTo(RESPONSE_STRING);
  }

  @Test
  public void testCacheDisabled() {
    loader.setCacheEnabled(false);
    loader.setStrategy(LoadStrategy.CACHE_FIRST);
    makeRequests();
    assertThat(server.getNumberRequests()).isEqualTo(3);
  }

  @Test
  public void testCacheEnabled() {
    loader.setCacheEnabled(true);
    loader.setStrategy(LoadStrategy.CACHE_FIRST);
    makeRequests();
    assertThat(server.getNumberRequests()).isEqualTo(1);
  }

  @Test
  public void testServerStrategy() {
    loader.setCacheEnabled(true);
    loader.setStrategy(LoadStrategy.SERVER_FIRST);
    makeRequests();
    assertThat(server.getNumberRequests()).isEqualTo(3);
  }

  @Test
  public void testCacheStrategyDisabled() {
    loader.setCacheEnabled(false);
    loader.setStrategy(LoadStrategy.CACHE_FIRST);
    makeRequests();
    assertThat(server.getNumberRequests()).isEqualTo(3);
  }

  private void makeRequests() {
    server.setMockResponseData(RESPONSE_STRING);
    assertThat(loader.loadString("/foo")).isEqualTo(RESPONSE_STRING);
    assertThat(loader.loadString("/foo")).isEqualTo(RESPONSE_STRING);
    assertThat(loader.loadString("/foo")).isEqualTo(RESPONSE_STRING);
  }

}
