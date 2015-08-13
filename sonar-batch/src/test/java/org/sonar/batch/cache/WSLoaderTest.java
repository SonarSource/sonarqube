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
package org.sonar.batch.cache;

import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.cache.WSLoader;
import org.sonar.batch.cache.WSLoader.LoadStrategy;
import org.hamcrest.Matchers;
import org.junit.rules.ExpectedException;
import org.junit.Rule;
import org.sonar.api.utils.HttpDownloader;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.apache.commons.io.IOUtils;
import org.mockito.Mockito;
import org.mockito.InOrder;

import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyInt;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.junit.Before;
import org.sonar.home.cache.PersistentCache;
import org.mockito.Mock;

public class WSLoaderTest {
  private final static String ID = "/dummy";
  private final static String cacheValue = "cache";
  private final static String serverValue = "server";

  @Mock
  private ServerClient client;
  @Mock
  private PersistentCache cache;
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    when(client.load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt())).thenReturn(IOUtils.toInputStream(serverValue));
    when(cache.get(ID, null)).thenReturn(cacheValue.getBytes());
    when(client.getURI(anyString())).thenAnswer(new Answer<URI>() {
      @Override
      public URI answer(InvocationOnMock invocation) throws Throwable {
        return new URI((String) invocation.getArguments()[0]);
      }
    });
  }

  @Test
  public void dont_retry_server_offline() throws IOException {
    turnServerOffline();
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);

    assertResult(loader.loadString(ID), cacheValue, true);
    assertResult(loader.loadString(ID), cacheValue, true);

    assertUsedServer(1);
    assertUsedCache(2);
  }

  @Test
  public void test_cache_strategy_fallback() throws IOException {
    turnCacheEmpty();
    WSLoader loader = new WSLoader(LoadStrategy.CACHE_FIRST, cache, client);

    assertResult(loader.load(ID), serverValue.getBytes(), false);

    InOrder inOrder = Mockito.inOrder(client, cache);
    inOrder.verify(cache).get(ID, null);
    inOrder.verify(client).load(eq(ID), anyString(), anyBoolean(), anyInt(), anyInt());
  }

  @Test
  public void test_server_strategy_fallback() throws IOException {
    turnServerOffline();
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);

    assertResult(loader.loadString(ID), cacheValue, true);

    InOrder inOrder = Mockito.inOrder(client, cache);
    inOrder.verify(client).load(eq(ID), anyString(), anyBoolean(), anyInt(), anyInt());
    inOrder.verify(cache).get(ID, null);
  }

  @Test
  public void test_put_cache() throws IOException {
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);
    loader.load(ID);
    verify(cache).put(ID, serverValue.getBytes());
  }

  @Test
  public void test_throw_cache_exception_fallback() throws IOException {
    turnServerOffline();

    when(cache.get(ID, null)).thenThrow(new NullPointerException());
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);

    try {
      loader.load(ID);
      fail("NPE expected");
    } catch (NullPointerException e) {
      assertUsedServer(1);
      assertUsedCache(1);
    }
  }

  @Test
  public void test_throw_cache_exception() throws IOException {
    when(cache.get(ID, null)).thenThrow(new IllegalStateException());

    WSLoader loader = new WSLoader(LoadStrategy.CACHE_FIRST, cache, client);

    try {
      loader.load(ID);
      fail("IllegalStateException expected");
    } catch (IllegalStateException e) {
      assertUsedServer(0);
      assertUsedCache(1);
    }
  }

  @Test
  public void test_throw_http_exceptions() {
    HttpDownloader.HttpException httpException = mock(HttpDownloader.HttpException.class);
    IllegalStateException wrapperException = new IllegalStateException(httpException);

    when(client.load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt())).thenThrow(wrapperException);

    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);

    try {
      loader.load(ID);
      fail("IllegalStateException expected");
    } catch (IllegalStateException e) {
      // cache should not be used
      verifyNoMoreInteractions(cache);
    }
  }

  @Test
  public void test_server_only_not_available() {
    turnServerOffline();

    exception.expect(IllegalStateException.class);
    exception.expectMessage(Matchers.is("Server is not available"));

    WSLoader loader = new WSLoader(LoadStrategy.SERVER_ONLY, cache, client);
    loader.load(ID);
  }

  @Test
  public void test_server_cache_not_available() throws IOException {
    turnServerOffline();
    turnCacheEmpty();

    exception.expect(IllegalStateException.class);
    exception.expectMessage(Matchers.is("Server is not accessible and data is not cached"));

    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);
    loader.load(ID);
  }

  @Test
  public void test_cache_only_available() throws IOException {
    turnCacheEmpty();

    exception.expect(IllegalStateException.class);
    exception.expectMessage(Matchers.is("Data is not cached"));

    WSLoader loader = new WSLoader(LoadStrategy.CACHE_ONLY, cache, client);
    loader.load(ID);
  }

  @Test
  public void test_server_strategy() throws IOException {
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);
    assertResult(loader.load(ID), serverValue.getBytes(), false);

    // should not fetch from cache
    verify(cache).put(ID, serverValue.getBytes());
    verifyNoMoreInteractions(cache);
  }

  @Test(expected = IllegalStateException.class)
  public void test_server_only() throws IOException {
    turnServerOffline();
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_ONLY, cache, client);
    loader.load(ID);
  }

  @Test
  public void test_string() {
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);
    assertResult(loader.loadString(ID), serverValue, false);
  }

  private void assertUsedCache(int times) throws IOException {
    verify(cache, times(times)).get(ID, null);
  }

  private void assertUsedServer(int times) {
    verify(client, times(times)).load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt());
  }

  private <T> void assertResult(WSLoaderResult<T> result, T expected, boolean fromCache) {
    assertThat(result).isNotNull();
    assertThat(result.get()).isEqualTo(expected);
    assertThat(result.isFromCache()).isEqualTo(fromCache);
  }

  private void turnServerOffline() {
    when(client.load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt())).thenThrow(new IllegalStateException());
  }

  private void turnCacheEmpty() throws IOException {
    when(cache.get(ID, null)).thenReturn(null);
  }
}
