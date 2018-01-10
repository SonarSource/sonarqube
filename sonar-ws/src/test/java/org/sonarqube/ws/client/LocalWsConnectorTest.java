/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.server.ws.LocalConnector;
import org.sonarqube.ws.MediaTypes;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocalWsConnectorTest {

  LocalConnector connector = mock(LocalConnector.class);
  LocalWsConnector underTest = new LocalWsConnector(connector);

  @Test
  public void baseUrl_is_always_slash() {
    assertThat(underTest.baseUrl()).isEqualTo("/");
  }

  @Test
  public void call_request() throws Exception {
    WsRequest wsRequest = new PostRequest("api/issues/search")
      .setMediaType(MediaTypes.JSON)
      .setParam("foo", "bar");
    answer(new DumbLocalResponse(400, MediaTypes.JSON, "{}".getBytes(UTF_8), Collections.<String>emptyList()));

    WsResponse wsResponse = underTest.call(wsRequest);

    verifyRequested("POST", "api/issues/search", MediaTypes.JSON, ImmutableMap.of("foo", "bar"));
    assertThat(wsResponse.code()).isEqualTo(400);
    assertThat(wsResponse.content()).isEqualTo("{}");
    assertThat(IOUtils.toString(wsResponse.contentReader())).isEqualTo("{}");
    assertThat(IOUtils.toString(wsResponse.contentStream())).isEqualTo("{}");
    assertThat(wsResponse.contentType()).isEqualTo(MediaTypes.JSON);
    assertThat(wsResponse.requestUrl()).isEqualTo("api/issues/search");
  }

  @Test
  public void call_request_with_defaults() throws Exception {
    // no parameters, no media type
    WsRequest wsRequest = new GetRequest("api/issues/search");
    answer(new DumbLocalResponse(200, MediaTypes.JSON, "".getBytes(UTF_8), Collections.<String>emptyList()));

    WsResponse wsResponse = underTest.call(wsRequest);

    verifyRequested("GET", "api/issues/search", MediaTypes.JSON, Collections.<String, String>emptyMap());
    assertThat(wsResponse.code()).isEqualTo(200);
    assertThat(wsResponse.content()).isEqualTo("");
    assertThat(IOUtils.toString(wsResponse.contentReader())).isEqualTo("");
    assertThat(IOUtils.toString(wsResponse.contentStream())).isEqualTo("");
    assertThat(wsResponse.contentType()).isEqualTo(MediaTypes.JSON);
  }

  private void answer(DumbLocalResponse response) {
    when(connector.call(any(LocalConnector.LocalRequest.class))).thenReturn(response);
  }

  private void verifyRequested(String expectedMethod, String expectedPath,
    String expectedMediaType, Map<String, String> expectedParams) {
    verify(connector).call(argThat(new ArgumentMatcher<LocalConnector.LocalRequest>() {
      @Override
      public boolean matches(LocalConnector.LocalRequest localRequest) {
        boolean ok = localRequest.getMethod().equals(expectedMethod) && localRequest.getPath().equals(expectedPath);
        ok &= localRequest.getMediaType().equals(expectedMediaType);
        for (Map.Entry<String, String> expectedParam : expectedParams.entrySet()) {
          String paramKey = expectedParam.getKey();
          ok &= localRequest.hasParam(paramKey);
          ok &= expectedParam.getValue().equals(localRequest.getParam(paramKey));
        }
        return ok;
      }
    }));
  }

  private static class DumbLocalResponse implements LocalConnector.LocalResponse {
    private final int code;
    private final String mediaType;
    private final byte[] bytes;
    private final List<String> headers;

    public DumbLocalResponse(int code, String mediaType, byte[] bytes, List<String> headers) {
      this.code = code;
      this.mediaType = mediaType;
      this.bytes = bytes;
      this.headers = headers;
    }

    @Override
    public int getStatus() {
      return code;
    }

    @Override
    public String getMediaType() {
      return mediaType;
    }

    @Override
    public byte[] getBytes() {
      return bytes;
    }

    @Override
    public Collection<String> getHeaderNames() {
      return headers;
    }

    @Override
    public String getHeader(String name) {
      throw new UnsupportedOperationException();
    }
  }
}
