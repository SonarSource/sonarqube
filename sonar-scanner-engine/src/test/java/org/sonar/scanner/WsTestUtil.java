/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner;

import java.io.InputStream;
import java.io.Reader;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.mockito.ArgumentMatcher;
import org.sonar.scanner.bootstrap.DefaultScannerWsClient;
import org.sonarqube.ws.client.WsRequest;
import org.sonarqube.ws.client.WsResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WsTestUtil {
  public static void mockStream(DefaultScannerWsClient mock, String path, InputStream is) {
    WsResponse response = mock(WsResponse.class);
    when(response.contentStream()).thenReturn(is);
    when(mock.call(argThat(new RequestMatcher(path)))).thenReturn(response);
  }

  public static void mockStream(DefaultScannerWsClient mock, InputStream is) {
    WsResponse response = mock(WsResponse.class);
    when(response.contentStream()).thenReturn(is);
    when(mock.call(any(WsRequest.class))).thenReturn(response);
  }

  public static void mockReader(DefaultScannerWsClient mock, Reader reader) {
    WsResponse response = mock(WsResponse.class);
    when(response.contentReader()).thenReturn(reader);
    when(mock.call(any(WsRequest.class))).thenReturn(response);
  }

  public static void mockReader(DefaultScannerWsClient mock, String path, Reader reader, Reader... others) {
    WsResponse response = mock(WsResponse.class);
    when(response.contentReader()).thenReturn(reader);
    WsResponse[] otherResponses = new WsResponse[others.length];
    for (int i = 0; i < others.length; i++) {
      WsResponse otherResponse = mock(WsResponse.class);
      when(otherResponse.contentReader()).thenReturn(others[i]);
      otherResponses[i] = otherResponse;
    }

    when(mock.call(argThat(new RequestMatcher(path)))).thenReturn(response, otherResponses);
  }

  public static void mockException(DefaultScannerWsClient mock, Exception e) {
    when(mock.call(any(WsRequest.class))).thenThrow(e);
  }

  public static void mockException(DefaultScannerWsClient mock, String path, Exception e) {
    when(mock.call(argThat(new RequestMatcher(path)))).thenThrow(e);
  }

  public static void verifyCall(DefaultScannerWsClient mock, String path) {
    verify(mock).call(argThat(new RequestMatcher(path)));
  }

  private static class RequestMatcher implements ArgumentMatcher<WsRequest> {
    private String path;

    public RequestMatcher(String path) {
      this.path = path;
    }

    @Override
    public boolean matches(@Nullable WsRequest item) {
      if (item == null) {
        return false;
      }
      return StringUtils.equals(item.getPath(), path);
    }
  }
}
