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
import java.io.InputStream;
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
  private final static String ID = "dummy";
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
    when(cache.getString(ID)).thenReturn(cacheValue);
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
  public void get_stream_from_cache() throws IOException {
    InputStream is = mock(InputStream.class);
    when(cache.getStream(ID)).thenReturn(is);
    WSLoader loader = new WSLoader(LoadStrategy.CACHE_FIRST, cache, client);
    WSLoaderResult<InputStream> result = loader.loadStream(ID);
    assertThat(result.get()).isEqualTo(is);
    verify(cache).getStream(ID);

    verifyNoMoreInteractions(cache, client);
  }

  @Test
  public void put_stream_in_cache() throws IOException {
    InputStream is1 = mock(InputStream.class);
    InputStream is2 = mock(InputStream.class);

    when(client.load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt())).thenReturn(is1);
    when(cache.getStream(ID)).thenReturn(is2);

    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);
    WSLoaderResult<InputStream> result = loader.loadStream(ID);
    assertThat(result.get()).isEqualTo(is2);

    verify(client).load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt());
    verify(cache).put(ID, is1);
    verify(cache).getStream(ID);

    verifyNoMoreInteractions(cache, client);
  }

  @Test
  public void test_cache_strategy_fallback() throws IOException {
    turnCacheEmpty();
    WSLoader loader = new WSLoader(LoadStrategy.CACHE_FIRST, cache, client);

    assertResult(loader.loadString(ID), serverValue, false);

    InOrder inOrder = Mockito.inOrder(client, cache);
    inOrder.verify(cache).getString(ID);
    inOrder.verify(client).load(eq(ID), anyString(), anyBoolean(), anyInt(), anyInt());
  }

  @Test
  public void test_server_strategy_fallback() throws IOException {
    turnServerOffline();
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);

    assertResult(loader.loadString(ID), cacheValue, true);

    InOrder inOrder = Mockito.inOrder(client, cache);
    inOrder.verify(client).load(eq(ID), anyString(), anyBoolean(), anyInt(), anyInt());
    inOrder.verify(cache).getString(ID);
  }

  @Test
  public void test_put_cache() throws IOException {
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);
    loader.loadString(ID);
    verify(cache).put(ID, serverValue.getBytes());
  }

  @Test
  public void test_throw_cache_exception_fallback() throws IOException {
    turnServerOffline();

    when(cache.getString(ID)).thenThrow(new NullPointerException());
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);

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

    WSLoader loader = new WSLoader(LoadStrategy.CACHE_FIRST, cache, client);

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
    HttpDownloader.HttpException httpException = mock(HttpDownloader.HttpException.class);
    IllegalStateException wrapperException = new IllegalStateException(httpException);

    when(client.load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt())).thenThrow(wrapperException);

    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);

    try {
      loader.loadString(ID);
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
    loader.loadString(ID);
  }

  @Test
  public void test_server_cache_not_available() throws IOException {
    turnServerOffline();
    turnCacheEmpty();

    exception.expect(IllegalStateException.class);
    exception.expectMessage(Matchers.is("Server is not accessible and data is not cached"));

    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);
    loader.loadString(ID);
  }

  @Test
  public void test_cache_only_available() throws IOException {
    turnCacheEmpty();

    exception.expect(IllegalStateException.class);
    exception.expectMessage(Matchers.is("Data is not cached"));

    WSLoader loader = new WSLoader(LoadStrategy.CACHE_ONLY, cache, client);
    loader.loadString(ID);
  }

  @Test
  public void test_server_strategy() throws IOException {
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);
    assertResult(loader.loadString(ID), serverValue, false);

    // should not fetch from cache
    verify(cache).put(ID, serverValue.getBytes());
    verifyNoMoreInteractions(cache);
  }

  @Test(expected = IllegalStateException.class)
  public void test_server_only() throws IOException {
    turnServerOffline();
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_ONLY, cache, client);
    loader.loadString(ID);
  }

  @Test
  public void test_string() {
    WSLoader loader = new WSLoader(LoadStrategy.SERVER_FIRST, cache, client);
    assertResult(loader.loadString(ID), serverValue, false);
  }

  private void assertUsedCache(int times) throws IOException {
    verify(cache, times(times)).getString(ID);
  }

  private void assertUsedServer(int times) {
    verify(client, times(times)).load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt());
  }

  private void assertResult(WSLoaderResult<InputStream> result, byte[] expected, boolean fromCache) throws IOException {
    byte[] content = IOUtils.toByteArray(result.get());
    assertThat(result).isNotNull();
    assertThat(content).isEqualTo(expected);
    assertThat(result.isFromCache()).isEqualTo(fromCache);
  }

  private void assertResult(WSLoaderResult<String> result, String expected, boolean fromCache) {
    assertThat(result).isNotNull();
    assertThat(result.get()).isEqualTo(expected);
    assertThat(result.isFromCache()).isEqualTo(fromCache);
  }

  private void turnServerOffline() {
    when(client.load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt())).thenThrow(new IllegalStateException());
  }

  private void turnCacheEmpty() throws IOException {
    when(cache.getString(ID)).thenReturn(null);
  }
}
