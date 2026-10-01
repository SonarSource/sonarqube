/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.web;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.server.ws.LocalConnector;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.security.WebSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class ApiV2LocalRequestDispatcherTest {

  private final ThreadLocalUserSession threadLocalUserSession = new ThreadLocalUserSession();
  private final ApiV2LocalRequestDispatcher underTest = new ApiV2LocalRequestDispatcher(threadLocalUserSession);
  private AnnotationConfigWebApplicationContext springContext;
  private DispatcherServlet dispatcherServlet;
  private MockServletContext servletContext;

  @BeforeEach
  void setUp() throws ServletException {
    servletContext = new MockServletContext();
    servletContext.setContextPath("/sonarqube");
    springContext = new AnnotationConfigWebApplicationContext();
    springContext.setServletContext(servletContext);
    springContext.register(TestWebConfig.class, WebSecurityConfig.class);
    springContext.refresh();
    dispatcherServlet = new DispatcherServlet(springContext);
    dispatcherServlet.init(new MockServletConfig(servletContext));
    underTest.init(servletContext, dispatcherServlet, springContext.getBean("springSecurityFilterChain", Filter.class));
    springContext.getBean(TestController.class).setLocalDispatcher(underTest);
  }

  @AfterEach
  void tearDown() {
    dispatcherServlet.destroy();
    springContext.close();
    threadLocalUserSession.unload();
    SecurityContextHolder.clearContext();
  }

  @Test
  void dispatch_whenGetWithParameters_shouldReturnControllerResponse() {
    threadLocalUserSession.set(new MockUserSession("john"));

    LocalConnector.LocalResponse response = underTest.dispatch(new TestLocalRequest("GET", "api/v2/test/echo")
      .setParam("name", "foo")
      .setParam("tags", "a", "b"));

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    assertThat(new String(response.getBytes(), StandardCharsets.UTF_8))
      .contains("\"name\":\"foo\"")
      .contains("\"tags\":[\"a\",\"b\"]")
      .contains("\"login\":\"john\"");
  }

  @Test
  void dispatch_whenPathHasLeadingSlash_shouldReturnControllerResponse() {
    threadLocalUserSession.set(new MockUserSession("john"));

    LocalConnector.LocalResponse response = underTest.dispatch(new TestLocalRequest("GET", "/api/v2/test/echo").setParam("name", "foo"));

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(new String(response.getBytes(), StandardCharsets.UTF_8)).contains("\"name\":\"foo\"");
  }

  @Test
  void dispatch_whenPost_shouldReturnControllerStatus() {
    LocalConnector.LocalResponse response = underTest.dispatch(new TestLocalRequest("POST", "api/v2/test/items").setParam("name", "foo"));

    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(new String(response.getBytes(), StandardCharsets.UTF_8)).isEqualTo("foo");
  }

  @Test
  void dispatch_whenEndpointRequiresAuthenticationAndCallerIsLoggedIn_shouldSucceed() {
    threadLocalUserSession.set(new MockUserSession("john"));

    LocalConnector.LocalResponse response = underTest.dispatch(new TestLocalRequest("GET", "api/v2/test/secured"));

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(new String(response.getBytes(), StandardCharsets.UTF_8)).isEqualTo("secret");
  }

  @Test
  void dispatch_whenEndpointRequiresAuthenticationAndCallerIsAnonymous_shouldBeRejected() {
    LocalConnector.LocalResponse response = underTest.dispatch(new TestLocalRequest("GET", "api/v2/test/secured"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getBytes()).isEmpty();
  }

  @Test
  void dispatch_whenPathHasTrailingSlash_shouldReturnControllerResponse() {
    threadLocalUserSession.set(new MockUserSession("john"));

    LocalConnector.LocalResponse response = underTest.dispatch(new TestLocalRequest("GET", "api/v2/test/echo/").setParam("name", "foo"));

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(new String(response.getBytes(), StandardCharsets.UTF_8)).contains("\"name\":\"foo\"");
  }

  @Test
  void dispatch_whenCalledFromApiV2Request_shouldReuseCallerUserSession() {
    threadLocalUserSession.set(new MockUserSession("john"));

    LocalConnector.LocalResponse response = underTest.dispatch(new TestLocalRequest("GET", "api/v2/test/nested"));

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(new String(response.getBytes(), StandardCharsets.UTF_8)).isEqualTo("200:secret:john");
  }

  @Test
  void dispatch_shouldRestoreCallerUserSession() {
    UserSession callerSession = new MockUserSession("john");
    threadLocalUserSession.set(callerSession);

    underTest.dispatch(new TestLocalRequest("GET", "api/v2/test/secured"));

    assertThat(threadLocalUserSession.get()).isSameAs(callerSession);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void dispatch_whenCallerHasNoUserSession_shouldNotLeaveAnyUserSession() {
    underTest.dispatch(new TestLocalRequest("GET", "api/v2/test/secured"));

    assertThat(threadLocalUserSession.hasSession()).isFalse();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void dispatch_shouldRestoreCallerSecurityContext() {
    SecurityContext callerSecurityContext = new SecurityContextImpl(new TestingAuthenticationToken("john", null));
    SecurityContextHolder.setContext(callerSecurityContext);
    threadLocalUserSession.set(new MockUserSession("john"));

    underTest.dispatch(new TestLocalRequest("GET", "api/v2/test/secured"));

    assertThat(SecurityContextHolder.getContext()).isSameAs(callerSecurityContext);
  }

  @Test
  void dispatch_whenEndpointFails_shouldReturnInternalErrorAndRestoreCallerUserSession() {
    UserSession callerSession = new MockUserSession("john");
    threadLocalUserSession.set(callerSession);

    LocalConnector.LocalResponse response = underTest.dispatch(new TestLocalRequest("GET", "api/v2/test/failure"));

    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    assertThat(new String(response.getBytes(), StandardCharsets.UTF_8))
      .isEqualTo("{\"errors\":[{\"msg\":\"An error has occurred. Please contact your administrator\"}]}");
    assertThat(threadLocalUserSession.get()).isSameAs(callerSession);
  }

  @Test
  void dispatch_whenEndpointDoesNotExist_shouldReturnNotFound() {
    LocalConnector.LocalResponse response = underTest.dispatch(new TestLocalRequest("GET", "api/v2/test/unknown"));

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void dispatch_whenNotInitialized_shouldReturnNotFound() {
    ApiV2LocalRequestDispatcher notInitialized = new ApiV2LocalRequestDispatcher(threadLocalUserSession);

    LocalConnector.LocalResponse response = notInitialized.dispatch(new TestLocalRequest("GET", "api/v2/test/echo"));

    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    assertThat(new String(response.getBytes(), StandardCharsets.UTF_8)).isEqualTo("{\"errors\":[{\"msg\":\"Unknown url : api/v2/test/echo\"}]}");
  }

  @Test
  void dispatch_whenSecurityFilterFails_shouldReturnInternalError() throws Exception {
    Filter failingFilter = mock(Filter.class);
    doThrow(new IllegalStateException("BOOM")).when(failingFilter).doFilter(any(), any(), any());
    underTest.init(servletContext, dispatcherServlet, failingFilter);

    LocalConnector.LocalResponse response = underTest.dispatch(new TestLocalRequest("GET", "api/v2/test/echo"));

    assertThat(response.getStatus()).isEqualTo(500);
  }

  @Configuration
  @EnableWebMvc
  static class TestWebConfig {
    @Bean
    ThreadLocalUserSession threadLocalUserSession() {
      return new ThreadLocalUserSession();
    }

    @Bean
    TestController testController(ThreadLocalUserSession userSession) {
      return new TestController(userSession);
    }
  }

  @RestController
  public static class TestController {
    private final ThreadLocalUserSession userSession;
    private ApiV2LocalRequestDispatcher localDispatcher;

    public TestController(ThreadLocalUserSession userSession) {
      this.userSession = userSession;
    }

    public void setLocalDispatcher(ApiV2LocalRequestDispatcher localDispatcher) {
      this.localDispatcher = localDispatcher;
    }

    @GetMapping("/test/nested")
    public String nested() {
      LocalConnector.LocalResponse response = localDispatcher.dispatch(new TestLocalRequest("GET", "api/v2/test/secured"));
      return response.getStatus() + ":" + new String(response.getBytes(), StandardCharsets.UTF_8) + ":" + userSession.getLogin();
    }

    @GetMapping(path = "/test/echo", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> echo(@RequestParam("name") String name, @RequestParam(name = "tags", required = false) List<String> tags) {
      Map<String, Object> result = new HashMap<>();
      result.put("name", name);
      result.put("tags", tags);
      result.put("login", userSession.getLogin());
      return result;
    }

    @PostMapping("/test/items")
    @ResponseStatus(HttpStatus.CREATED)
    public String create(@RequestParam("name") String name) {
      return name;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/test/secured")
    public String secured() {
      return "secret";
    }

    @GetMapping("/test/failure")
    public String failure() {
      throw new IllegalStateException("BOOM");
    }
  }

  private static class TestLocalRequest implements LocalConnector.LocalRequest {
    private final String method;
    private final String path;
    private final Map<String, String[]> params = new HashMap<>();

    TestLocalRequest(String method, String path) {
      this.method = method;
      this.path = path;
    }

    TestLocalRequest setParam(String key, String... values) {
      params.put(key, values);
      return this;
    }

    @Override
    public String getPath() {
      return path;
    }

    @Override
    public String getMediaType() {
      return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public String getMethod() {
      return method;
    }

    @Override
    public boolean hasParam(String key) {
      return params.containsKey(key);
    }

    @Override
    public String getParam(String key) {
      return hasParam(key) ? params.get(key)[0] : null;
    }

    @Override
    public List<String> getMultiParam(String key) {
      return hasParam(key) ? List.of(params.get(key)) : List.of();
    }

    @Override
    public Optional<String> getHeader(String name) {
      return Optional.empty();
    }

    @Override
    public Map<String, String[]> getParameterMap() {
      return params;
    }
  }
}
