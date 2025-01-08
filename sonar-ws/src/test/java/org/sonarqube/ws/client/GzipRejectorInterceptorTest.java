/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonarqube.ws.client;

import java.io.IOException;
import java.util.List;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GzipRejectorInterceptorTest {

  private final GzipRejectorInterceptor underTest = new GzipRejectorInterceptor();

  private Interceptor.Chain chain = mock();
  private Response response = mock(Response.class);
  private Request request = mock(Request.class);
  private Request.Builder builderThatRemovesHeaders = mock(Request.Builder.class);

  @Before
  public void before() throws IOException {
    when(builderThatRemovesHeaders.removeHeader(any())).thenReturn(builderThatRemovesHeaders);
    when(builderThatRemovesHeaders.build()).thenReturn(request);
    when(request.newBuilder()).thenReturn(builderThatRemovesHeaders);
    when(chain.request()).thenReturn(request);
    when(chain.proceed(any())).thenReturn(response);
  }

  @Test
  public void intercept_shouldAlwaysRemoveAcceptEncoding() throws IOException {
    underTest.intercept(chain);

    verify(builderThatRemovesHeaders, times(1)).removeHeader("Accept-Encoding");
  }

  @Test
  public void intercept_whenGzipContentEncodingIncluded_shouldCloseTheResponse() throws IOException {
    when(response.headers("Content-Encoding")).thenReturn(List.of("gzip"));

    underTest.intercept(chain);

    verify(response, times(1)).close();
  }

  @Test
  public void intercept_whenGzipContentEncodingNotIncluded_shouldNotCloseTheResponse() throws IOException {
    when(response.headers()).thenReturn(Headers.of("Custom-header", "not-gzip"));

    underTest.intercept(chain);

    verify(response, times(0)).close();
  }
}