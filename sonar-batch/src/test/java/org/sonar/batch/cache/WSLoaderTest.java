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

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.batch.bootstrap.BatchWsClient;
import org.sonar.batch.cache.WSLoader.LoadStrategy;
import org.sonar.home.cache.PersistentCache;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.MockWsResponse;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.client.WsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class WSLoaderTest {
  private final static String ID = "dummy";
  private final static String cacheValue = "cache";
  private final static String serverValue = "server";

  @Rule
  public ExpectedException exception = ExpectedException.none();

  BatchWsClient ws = mock(BatchWsClient.class, Mockito.RETURNS_DEEP_STUBS);
  PersistentCache cache = mock(PersistentCache.class);

  @Test
  public void dont_retry_server_offline() throws IOException {
    turnServerOffline();
    when(cache.getString(ID)).thenReturn(cacheValue);
    WSLoader underTest = new WSLoader(LoadStrategy.SERVER_FIRST, cache, ws);

    assertResult(underTest.loadString(ID), cacheValue, true);
    assertResult(underTest.loadString(ID), cacheValue, true);

    assertUsedServer(1);
    assertUsedCache(2);
  }

  @Test
  public void get_stream_from_cache() throws IOException {
    InputStream is = IOUtils.toInputStream("is");
    when(cache.getStream(ID)).thenReturn(is);

    WSLoader loader = new WSLoader(LoadStrategy.CACHE_FIRST, cache, ws);
    WSLoaderResult<InputStream> result = loader.loadStream(ID);

    assertThat(result.get()).isEqualTo(is);
    verify(cache).getStream(ID);
    verifyNoMoreInteractions(cache, ws);
  }

  @Test
  public void put_stream_in_cache() throws IOException {
    InputStream input = IOUtils.toInputStream("is");

    when(ws.call(any(WsRequest.class))).thenReturn(new MockWsResponse().setContent(input));
    when(cache.getStream(ID)).thenReturn(input);

    // SERVER_FIRST -> load from server then put to cache
    WSLoader underTest = new WSLoader(LoadStrategy.SERVER_FIRST, cache, ws);
    WSLoaderResult<InputStream> result = underTest.loadStream(ID);
    assertThat(result.get()).isEqualTo(input);

    InOrder inOrder = inOrder(ws, cache);
    inOrder.verify(ws).call(any(WsRequest.class));
    inOrder.verify(cache).put(eq(ID), any(InputStream.class));
    inOrder.verify(cache).getStream(ID);
    verifyNoMoreInteractions(cache, ws);
  }

  @Test
  public void test_cache_strategy_fallback() throws IOException {
    turnCacheEmpty();
    when(ws.call(any(WsRequest.class))).thenReturn(new MockWsResponse().setContent(serverValue));
    WSLoader loader = new WSLoader(LoadStrategy.CACHE_FIRST, cache, ws);

    assertResult(loader.loadString(ID), serverValue, false);

    InOrder inOrder = inOrder(ws, cache);
    inOrder.verify(cache).getString(ID);
    inOrder.verify(ws).call(any(WsRequest.class));
  }

  @Test
  public void test_server_strategy_fallback() throws IOException {
    turnServerOffline();
    when(cache.getString(ID)).thenReturn(cacheValue);
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, ws);

    assertResult(loader.loadString(ID), cacheValue, true);

    InOrder inOrder = inOrder(ws, cache);
    inOrder.verify(ws).call(any(WsRequest.class));
    inOrder.verify(cache).getString(ID);
  }

  @Test
  public void test_put_cache() throws IOException {
    when(ws.call(any(WsRequest.class))).thenReturn(new MockWsResponse().setContent(serverValue));
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, ws);
    loader.loadString(ID);
    verify(cache).put(ID, serverValue.getBytes());
  }

  @Test
  public void test_throw_cache_exception_fallback() throws IOException {
    turnServerOffline();

    when(cache.getString(ID)).thenThrow(new NullPointerException());
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, ws);

    try {
      loader.loadString(ID);
      fail("NPE expected");
    } catch (NullPointerException e) {
      assertUsedServer(1);
      assertUsedCache(1);
    }
  }

  @Test
  public void test_throw_cache_exception() throws IOException {
    when(cache.getString(ID)).thenThrow(new IllegalStateException());

    WSLoader loader = new WSLoader(LoadStrategy.CACHE_FIRST, cache, ws);

    try {
      loader.loadString(ID);
      fail("IllegalStateException expected");
    } catch (IllegalStateException e) {
      assertUsedServer(0);
      assertUsedCache(1);
    }
  }

  @Test
  public void test_throw_http_exceptions() {
    when(ws.call(any(WsRequest.class))).thenThrow(new HttpException("url", 500));

    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, ws);

    try {
      loader.loadString(ID);
      fail("IllegalStateException expected");
    } catch (HttpException e) {
      // cache should not be used
      verifyNoMoreInteractions(cache);
    }
  }

  @Test
  public void test_server_only_not_available() {
    turnServerOffline();

    exception.expect(IllegalStateException.class);
    exception.expectMessage(Matchers.containsString("Server is not available"));

    WSLoader loader = new WSLoader(LoadStrategy.SERVER_ONLY, cache, ws);
    loader.loadString(ID);
  }

  @Test
  public void test_server_cache_not_available() throws IOException {
    turnServerOffline();
    turnCacheEmpty();

    exception.expect(IllegalStateException.class);
    exception.expectMessage(Matchers.is("Server is not accessible and data is not cached"));

    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, ws);
    loader.loadString(ID);
  }

  @Test
  public void test_cache_only_available() throws IOException {
    turnCacheEmpty();

    exception.expect(IllegalStateException.class);
    exception.expectMessage(Matchers.is("Data is not cached"));

    WSLoader loader = new WSLoader(LoadStrategy.CACHE_ONLY, cache, ws);
    loader.loadString(ID);
  }

  @Test
  public void test_server_strategy() throws IOException {
    when(ws.call(any(WsRequest.class))).thenReturn(new MockWsResponse().setContent(serverValue));
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, ws);
    assertResult(loader.loadString(ID), serverValue, false);

    // should not fetch from cache
    verify(cache).put(ID, serverValue.getBytes());
    verifyNoMoreInteractions(cache);
  }

  @Test(expected = IllegalStateException.class)
  public void test_server_only() throws IOException {
    turnServerOffline();
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_ONLY, cache, ws);
    loader.loadString(ID);
  }

  @Test
  public void test_string() {
    when(ws.call(any(WsRequest.class))).thenReturn(new MockWsResponse().setContent(serverValue));
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, ws);
    assertResult(loader.loadString(ID), serverValue, false);
  }

  private void assertUsedCache(int times) throws IOException {
    verify(cache, times(times)).getString(ID);
  }

  private void assertUsedServer(int times) {
    verify(ws, times(times)).call(any(WsRequest.class));
  }

  private void assertResult(WSLoaderResult<String> result, String expected, boolean fromCache) {
    assertThat(result).isNotNull();
    assertThat(result.get()).isEqualTo(expected);
    assertThat(result.isFromCache()).isEqualTo(fromCache);
  }

  private void turnServerOffline() {
    when(ws.call(any(WsRequest.class))).thenThrow(new IllegalStateException());
  }

  private void turnCacheEmpty() throws IOException {
    when(cache.getString(ID)).thenReturn(null);
  }
}
