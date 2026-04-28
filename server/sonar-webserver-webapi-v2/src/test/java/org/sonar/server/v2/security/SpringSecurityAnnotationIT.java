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
package org.sonar.server.v2.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.AnonymousMockUserSession;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test demonstrating Spring Security annotations working with full Spring context.
 *
 * This test uses:
 * - @WebAppConfiguration + @ContextConfiguration for Spring test context
 * - Full Spring Web MVC context with AOP enabled
 * - WebSecurityConfig for Spring Security
 * - Custom test configuration to wire UserSession
 *
 * Unlike standalone MockMvc tests, this:
 * ✅ Enforces @PreAuthorize and @RequireAuthentication annotations
 * ✅ Has Spring Security filter chain active
 * ✅ Uses Spring AOP for method security
 * ✅ Populates SecurityContext from UserSession
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
  SpringSecurityAnnotationIT.TestConfig.class,
  WebSecurityConfig.class
})
public class SpringSecurityAnnotationIT {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private ThreadLocalUserSession threadLocalUserSession;

  private MockMvc mockMvc;

  @Before
  public void setUp() {
    // Build MockMvc with full Spring context (includes Spring Security)
    // The addFilters() ensures Spring Security filter chain is applied
    mockMvc = MockMvcBuilders
      .webAppContextSetup(webApplicationContext)
      .addFilters(new UserSessionAuthenticationFilter(threadLocalUserSession))
      .build();

    // Clean up any previous session
    threadLocalUserSession.unload();
  }

  @Test
  public void publicEndpoint_withoutAuthentication_shouldSucceed() throws Exception {
    // No authentication needed

    mockMvc.perform(get("/test-security/public"))
      .andExpectAll(
        status().isOk(),
        content().string("public"));
  }

  @Test
  public void preAuthorizeIsAuthenticated_withoutAuthentication_shouldFail() {
    assertThatThrownBy(() -> mockMvc.perform(get("/test-security/authenticated")))
      .hasCauseInstanceOf(AccessDeniedException.class);
  }

  @Test
  public void preAuthorizeIsAuthenticated_withAuthentication_shouldSucceed() throws Exception {
    MockUserSession userSession = new MockUserSession("user");
    threadLocalUserSession.set(userSession);

    mockMvc.perform(get("/test-security/authenticated"))
      .andExpectAll(
        status().isOk(),
        content().string("authenticated"));
  }

  @Test
  public void preAuthorizeHasRoleAdmin_withoutAuthentication_shouldFail() {
    assertThatThrownBy(() -> mockMvc.perform(get("/test-security/admin")))
      .hasCauseInstanceOf(AccessDeniedException.class);
  }

  @Test
  public void preAuthorizeHasRoleAdmin_withNonAdminUser_shouldFail() {
    assertAdminEndpointDeniesNonAdmin("/test-security/admin", "AuthorizationDeniedException");
  }

  @Test
  public void preAuthorizeHasRoleAdmin_withAdminUser_shouldSucceed() throws Exception {
    assertAdminEndpointAllowsAdmin("/test-security/admin", "admin");
  }

  @Test
  public void requireAdmin_withNonAdminUser_shouldFail() {
    assertAdminEndpointDeniesNonAdmin("/test-security/require-admin", "AuthorizationDeniedException");
  }

  @Test
  public void requireAdmin_withAdminUser_shouldSucceed() throws Exception {
    assertAdminEndpointAllowsAdmin("/test-security/require-admin", "require-admin");
  }

  @Test
  public void manualSecurityCheck_withNonAdminUser_shouldFail() {
    assertAdminEndpointDeniesNonAdmin("/test-security/manual-admin", "ForbiddenException");
  }

  @Test
  public void manualSecurityCheck_withAdminUser_shouldSucceed() throws Exception {
    assertAdminEndpointAllowsAdmin("/test-security/manual-admin", "manual-admin");
  }

  @Test
  public void bridgeSecurityCheck_withNonAdminUser_shouldFail() {
    assertAdminEndpointDeniesNonAdmin("/test-security/bridge-admin", "ForbiddenException");
  }

  @Test
  public void bridgeSecurityCheck_withAdminUser_shouldSucceed() throws Exception {
    assertAdminEndpointAllowsAdmin("/test-security/bridge-admin", "bridge-admin");
  }

  private void assertAdminEndpointDeniesNonAdmin(String endpoint, String expectedExceptionType) {
    MockUserSession userSession = new MockUserSession("user");
    userSession.setSystemAdministrator(false);
    threadLocalUserSession.set(userSession);

    if ("AuthorizationDeniedException".equals(expectedExceptionType)) {
      assertThatThrownBy(() -> mockMvc.perform(get(endpoint)))
        .hasCauseInstanceOf(AccessDeniedException.class);
    } else if ("ForbiddenException".equals(expectedExceptionType)) {
      assertThatThrownBy(() -> mockMvc.perform(get(endpoint)))
        .hasCauseInstanceOf(ForbiddenException.class);
    }
  }

  private void assertAdminEndpointAllowsAdmin(String endpoint, String expectedResponse) throws Exception {
    MockUserSession userSession = new MockUserSession("admin");
    userSession.setSystemAdministrator(true);
    threadLocalUserSession.set(userSession);

    mockMvc.perform(get(endpoint))
      .andExpectAll(
        status().isOk(),
        content().string(expectedResponse));
  }

  @Test
  public void requireAuthentication_withoutAuthentication_shouldFail() {
    assertThatThrownBy(() -> mockMvc.perform(get("/test-security/require-auth")))
      .hasCauseInstanceOf(AccessDeniedException.class);
  }

  @Test
  public void requireAuthentication_withAuthentication_shouldSucceed() throws Exception {
    MockUserSession userSession = new MockUserSession("user");
    threadLocalUserSession.set(userSession);

    mockMvc.perform(get("/test-security/require-auth"))
      .andExpectAll(
        status().isOk(),
        content().string("require-auth"));
  }

  @Test
  public void requireAdmin_withoutAuthentication_shouldFail() {
    assertThatThrownBy(() -> mockMvc.perform(get("/test-security/require-admin")))
      .hasCauseInstanceOf(AccessDeniedException.class);
  }

  @Test
  public void anonymousUser_shouldNotBeAuthenticated() {
    AnonymousMockUserSession userSession = new AnonymousMockUserSession();
    threadLocalUserSession.set(userSession);

    assertThatThrownBy(() -> mockMvc.perform(get("/test-security/authenticated")))
      .hasCauseInstanceOf(AccessDeniedException.class);
  }

  @Test
  public void manualSecurityCheck_withoutAuthentication_shouldFail() {
    assertThatThrownBy(() -> mockMvc.perform(get("/test-security/manual-admin")))
      .hasCauseInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void bridgeSecurityCheck_withoutAuthentication_shouldFail() {
    assertThatThrownBy(() -> mockMvc.perform(get("/test-security/bridge-admin")))
      .hasMessageContaining("No UserSession found in SecurityContext");
  }

  /**
   * Test controller with Spring Security annotations.
   */
  @RestController
  static class TestSecurityController {

    private final UserSession userSession;

    TestSecurityController(UserSession userSession) {
      this.userSession = userSession;
    }

    @GetMapping("/test-security/public")
    public String publicEndpoint() {
      return "public";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/test-security/authenticated")
    public String authenticatedEndpoint() {
      return "authenticated";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test-security/admin")
    public String adminEndpoint() {
      return "admin";
    }

    @RequireAuthentication
    @GetMapping("/test-security/require-auth")
    public String requireAuthEndpoint() {
      return "require-auth";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test-security/require-admin")
    public String requireAdminEndpoint() {
      return "require-admin";
    }

    @GetMapping("/test-security/manual-admin")
    public String manualAdminEndpoint() {
      // Traditional manual security check (no Spring Security annotations)
      // Uses injected UserSession (delegates to ThreadLocalUserSession)
      userSession.checkLoggedIn().checkIsSystemAdministrator();
      return "manual-admin";
    }

    @GetMapping("/test-security/bridge-admin")
    public String bridgeAdminEndpoint() {
      // Demonstrates the bridge: Get UserSession from Spring Security's SecurityContext
      // This proves ThreadLocalUserSession → SecurityContext bridge works
      var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

      if (authentication == null || authentication.getPrincipal() == null) {
        throw new IllegalStateException("No UserSession found in SecurityContext");
      }

      // Extract UserSession from SonarUserDetails principal
      var principal = authentication.getPrincipal();
      if (!(principal instanceof SonarUserDetails)) {
        throw new IllegalStateException("No UserSession found in SecurityContext");
      }

      SonarUserDetails userDetails = (SonarUserDetails) principal;
      UserSession sessionFromContext = userDetails.getUserSession();
      sessionFromContext.checkLoggedIn().checkIsSystemAdministrator();
      return "bridge-admin";
    }
  }

  /**
   * Test configuration that sets up the necessary beans for security integration.
   */
  @Configuration
  @EnableWebMvc
  static class TestConfig {

    /**
     * ThreadLocalUserSession bean for storing user context.
     */
    @Bean
    public ThreadLocalUserSession threadLocalUserSession() {
      return new ThreadLocalUserSession();
    }

    /**
     * UserSession bean that delegates to ThreadLocalUserSession.
     * This is what gets injected into controllers.
     */
    @Bean
    @Primary
    public UserSession userSession(ThreadLocalUserSession threadLocalUserSession) {
      return threadLocalUserSession;
    }

    /**
     * Test controller bean.
     */
    @Bean
    public TestSecurityController testSecurityController(UserSession userSession) {
      return new TestSecurityController(userSession);
    }
  }
}
