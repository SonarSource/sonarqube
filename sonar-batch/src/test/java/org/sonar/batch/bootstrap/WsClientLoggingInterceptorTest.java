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

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WsClientLoggingInterceptorTest {

  @Rule
  public LogTester logTester = new LogTester();

  WsClientLoggingInterceptor underTest = new WsClientLoggingInterceptor();
  Interceptor.Chain chain = mock(Interceptor.Chain.class);

  @Test
  public void log_and_profile_request_if_debug_level() throws Exception {
    Request request = newRequest();
    Response response = newResponse(request, 200, "");
    when(chain.request()).thenReturn(request);
    when(chain.proceed(request)).thenReturn(response);

    logTester.setLevel(LoggerLevel.DEBUG);
    Response result = underTest.intercept(chain);

    // do not fail the execution -> interceptor returns the response
    assertThat(result).isSameAs(response);

    // check logs
    List<String> debugLogs = logTester.logs(LoggerLevel.DEBUG);
    assertThat(debugLogs).hasSize(1);
    assertThat(debugLogs.get(0)).contains("GET 200 https://localhost:9000/api/issues/search | time=");
    List<String> traceLogs = logTester.logs(LoggerLevel.TRACE);
    assertThat(traceLogs).hasSize(1);
    assertThat(traceLogs.get(0)).isEqualTo("GET https://localhost:9000/api/issues/search");
  }

  @Test
  public void fail_if_requires_authentication() throws Exception {
    Request request = newRequest();
    Response response = newResponse(request, 401, "");
    when(chain.request()).thenReturn(request);
    when(chain.proceed(request)).thenReturn(response);

    try {
      underTest.intercept(chain);
      fail();
    } catch (MessageException e) {
      assertThat(e.getMessage()).isEqualTo(
        "Not authorized. Analyzing this project requires to be authenticated. Please provide the values of the properties sonar.login and sonar.password.");
    }
  }

  @Test
  public void fail_if_credentials_are_not_valid() throws Exception {
    Request request = new Request.Builder()
      .url("https://localhost:9000/api/issues/search")
      .header("Authorization", "Basic BAD_CREDENTIALS")
      .get()
      .build();
    Response response = newResponse(request, 401, "");
    when(chain.request()).thenReturn(request);
    when(chain.proceed(request)).thenReturn(response);

    try {
      underTest.intercept(chain);
      fail();
    } catch (MessageException e) {
      assertThat(e.getMessage()).isEqualTo(
        "Not authorized. Please check the properties sonar.login and sonar.password.");
    }
  }

  @Test
  public void fail_if_requires_permission() throws Exception {
    Request request = newRequest();
    Response response = newResponse(request, 403, "{\"errors\":[{\"msg\":\"missing scan permission\"}, {\"msg\":\"missing another permission\"}]}");
    when(chain.request()).thenReturn(request);
    when(chain.proceed(request)).thenReturn(response);

    try {
      underTest.intercept(chain);
      fail();
    } catch (MessageException e) {
      assertThat(e.getMessage()).isEqualTo("missing scan permission, missing another permission");
    }
  }

  private Request newRequest() {
    return new Request.Builder().url("https://localhost:9000/api/issues/search").get().build();
  }

  private Response newResponse(Request getRequest, int code, String jsonBody) {
    return new Response.Builder().request(getRequest)
      .code(code)
      .protocol(Protocol.HTTP_1_1)
      .body(ResponseBody.create(MediaType.parse("application/json"), jsonBody))
      .build();
  }

}
