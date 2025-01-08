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
package org.sonar.ce.httpd;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public final class CeHttpUtils {
  private CeHttpUtils() {
    // prevents instantiation
  }

  private static HttpResponse performRequestOnHandler(HttpAction handler, HttpRequest request) throws HttpException {
    HttpResponse mockResponse = mock(HttpResponse.class);
    HttpContext mockContext = mock(HttpContext.class);
    handler.handle(request, mockResponse, mockContext);
    return mockResponse;
  }

  @NotNull
  private static HttpRequest buildGetRequest() {
    return new BasicHttpRequest("GET", "");
  }

  @NotNull
  private static HttpPost buildPostRequest(List<NameValuePair> urlArgs, List<NameValuePair> bodyArgs) {
    final URI requestUri;
    try {
      requestUri = new URIBuilder()
        .setScheme("http")
        .setHost("localhost")
        .addParameters(urlArgs)
        .build();
    } catch (URISyntaxException urise) {
      throw new IllegalStateException("built URI is not valid, this should not be possible", urise);
    }
    HttpPost request = new HttpPost(requestUri);
    request.setEntity(new UrlEncodedFormEntity(bodyArgs, StandardCharsets.UTF_8));
    return request;
  }

  private static byte[] extractResponseBody(HttpResponse mockResponse) throws IOException {
    final ArgumentCaptor<HttpEntity> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(mockResponse).setEntity(httpEntityCaptor.capture());
    return IOUtils.buffer(httpEntityCaptor.getValue().getContent()).readAllBytes();
  }

  private static void verifyStatusCode(int expectedCode, HttpResponse mockResponse) {
    verify(mockResponse).setStatusLine(any(), eq(expectedCode));
  }

  public static void testHandlerForGetWithoutResponseBody(HttpAction handler, int expectedCode) throws HttpException {
    HttpRequest request = buildGetRequest();
    HttpResponse mockResponse = performRequestOnHandler(handler, request);
    verifyStatusCode(expectedCode, mockResponse);
  }

  public static byte[] testHandlerForGetWithResponseBody(HttpAction handler, int expectedCode) throws HttpException, IOException {
    HttpRequest request = buildGetRequest();
    HttpResponse mockResponse = performRequestOnHandler(handler, request);
    verifyStatusCode(expectedCode, mockResponse);
    return extractResponseBody(mockResponse);
  }

  public static void testHandlerForPostWithoutResponseBody(
    HttpAction httpAction, List<NameValuePair> urlArgs, List<NameValuePair> bodyArgs, int expectedCode) throws HttpException {
    HttpPost request = buildPostRequest(urlArgs, bodyArgs);
    HttpResponse mockResponse = performRequestOnHandler(httpAction, request);
    verifyStatusCode(expectedCode, mockResponse);
  }

  public static byte[] testHandlerForPostWithResponseBody(
    HttpAction httpAction, List<NameValuePair> urlArgs, List<NameValuePair> bodyArgs, int expectedCode) throws HttpException, IOException {
    HttpPost request = buildPostRequest(urlArgs, bodyArgs);
    HttpResponse mockResponse = performRequestOnHandler(httpAction, request);
    verifyStatusCode(expectedCode, mockResponse);
    return extractResponseBody(mockResponse);
  }
}
