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
import org.sonar.db.permission.GlobalPermission;
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
 * Integration test proving backward compatibility with existing V2 endpoints that use
 * manual UserSession checks instead of Spring Security annotations.
 *
 * <p>This test verifies that endpoints using traditional patterns like:</p>
 * <ul>
 *   <li>{@code userSession.checkLoggedIn()} - manual authentication check</li>
 *   <li>{@code userSession.checkIsSystemAdministrator()} - manual admin check</li>
 *   <li>{@code userSession.hasPermission()} - manual permission check</li>
 * </ul>
 *
 * <p>Continue to work correctly with the new SecurityContextBackedUserSession bridge,
 * ensuring existing V2 endpoints don't break when Spring Security integration is added.</p>
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
  UserSessionBackwardCompatibilityIT.TestConfig.class,
  WebSecurityConfig.class
})
public class UserSessionBackwardCompatibilityIT {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private ThreadLocalUserSession threadLocalUserSession;

  private MockMvc mockMvc;

  @Before
  public void setUp() {
    mockMvc = MockMvcBuilders
      .webAppContextSetup(webApplicationContext)
      .addFilters(new UserSessionAuthenticationFilter(threadLocalUserSession))
      .build();

    threadLocalUserSession.unload();
  }

  @Test
  public void legacyEndpoint_withoutAuthentication_shouldFailWithUnauthorized() {
    assertThatThrownBy(() -> mockMvc.perform(get("/test-legacy/authenticated")))
      .hasCauseInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void legacyEndpoint_withAuthentication_shouldSucceed() throws Exception {
    MockUserSession userSession = new MockUserSession("user");
    threadLocalUserSession.set(userSession);

    mockMvc.perform(get("/test-legacy/authenticated"))
      .andExpectAll(
        status().isOk(),
        content().string("authenticated-user"));
  }

  @Test
  public void legacyAdminEndpoint_withNonAdminUser_shouldFailWithForbidden() {
    MockUserSession userSession = new MockUserSession("user");
    userSession.setSystemAdministrator(false);
    threadLocalUserSession.set(userSession);

    assertThatThrownBy(() -> mockMvc.perform(get("/test-legacy/admin")))
      .hasCauseInstanceOf(ForbiddenException.class);
  }

  @Test
  public void legacyAdminEndpoint_withAdminUser_shouldSucceed() throws Exception {
    MockUserSession userSession = new MockUserSession("admin");
    userSession.setSystemAdministrator(true);
    threadLocalUserSession.set(userSession);

    mockMvc.perform(get("/test-legacy/admin"))
      .andExpectAll(
        status().isOk(),
        content().string("admin-user"));
  }

  @Test
  public void legacyPermissionCheck_withoutPermission_shouldFailWithForbidden() {
    MockUserSession userSession = new MockUserSession("user");
    userSession.setSystemAdministrator(false);
    threadLocalUserSession.set(userSession);

    assertThatThrownBy(() -> mockMvc.perform(get("/test-legacy/permission")))
      .hasCauseInstanceOf(ForbiddenException.class);
  }

  @Test
  public void legacyPermissionCheck_withPermission_shouldSucceed() throws Exception {
    MockUserSession userSession = new MockUserSession("user");
    userSession.addPermission(GlobalPermission.ADMINISTER);
    threadLocalUserSession.set(userSession);

    mockMvc.perform(get("/test-legacy/permission"))
      .andExpectAll(
        status().isOk(),
        content().string("user-with-permission"));
  }

  @Test
  public void legacyEndpoint_canAccessUserSessionData() throws Exception {
    MockUserSession userSession = new MockUserSession("john.doe")
      .setUuid("user-uuid-123")
      .setName("John Doe");
    threadLocalUserSession.set(userSession);

    mockMvc.perform(get("/test-legacy/user-data"))
      .andExpectAll(
        status().isOk(),
        content().string("login=john.doe,uuid=user-uuid-123,name=John Doe"));
  }

  @Test
  public void legacyEndpoint_withAnonymousUser_shouldReturnAnonymousData() throws Exception {
    AnonymousMockUserSession userSession = new AnonymousMockUserSession();
    threadLocalUserSession.set(userSession);

    mockMvc.perform(get("/test-legacy/user-info"))
      .andExpectAll(
        status().isOk(),
        content().string("loggedIn=false,admin=false"));
  }

  @Test
  public void legacyEndpoint_withAuthenticatedUser_shouldReturnUserData() throws Exception {
    MockUserSession userSession = new MockUserSession("user")
      .setSystemAdministrator(false);
    threadLocalUserSession.set(userSession);

    mockMvc.perform(get("/test-legacy/user-info"))
      .andExpectAll(
        status().isOk(),
        content().string("loggedIn=true,admin=false"));
  }

  @Test
  public void legacyEndpoint_withAdminUser_shouldReturnAdminData() throws Exception {
    MockUserSession userSession = new MockUserSession("admin")
      .setSystemAdministrator(true);
    threadLocalUserSession.set(userSession);

    mockMvc.perform(get("/test-legacy/user-info"))
      .andExpectAll(
        status().isOk(),
        content().string("loggedIn=true,admin=true"));
  }

  /**
   * Test controller using ONLY traditional UserSession checks - no Spring Security annotations.
   * This simulates existing V2 endpoints that rely on manual security checks.
   */
  @RestController
  static class LegacySecurityController {

    private final UserSession userSession;

    LegacySecurityController(UserSession userSession) {
      this.userSession = userSession;
    }

    @GetMapping("/test-legacy/authenticated")
    public String authenticatedEndpoint() {
      userSession.checkLoggedIn();
      return "authenticated-user";
    }

    @GetMapping("/test-legacy/admin")
    public String adminEndpoint() {
      userSession.checkLoggedIn().checkIsSystemAdministrator();
      return "admin-user";
    }

    @GetMapping("/test-legacy/permission")
    public String permissionEndpoint() {
      userSession.checkPermission(GlobalPermission.ADMINISTER);
      return "user-with-permission";
    }

    @GetMapping("/test-legacy/user-data")
    public String userDataEndpoint() {
      userSession.checkLoggedIn();
      return String.format("login=%s,uuid=%s,name=%s",
        userSession.getLogin(),
        userSession.getUuid(),
        userSession.getName());
    }

    @GetMapping("/test-legacy/user-info")
    public String userInfoEndpoint() {
      return String.format("loggedIn=%s,admin=%s",
        userSession.isLoggedIn(),
        userSession.isSystemAdministrator());
    }
  }

  /**
   * Test configuration that sets up the necessary beans for security integration.
   */
  @Configuration
  @EnableWebMvc
  static class TestConfig {

    @Bean
    public ThreadLocalUserSession threadLocalUserSession() {
      return new ThreadLocalUserSession();
    }

    @Bean
    @Primary
    public UserSession userSession(ThreadLocalUserSession threadLocalUserSession) {
      return threadLocalUserSession;
    }

    @Bean
    public LegacySecurityController legacySecurityController(UserSession userSession) {
      return new LegacySecurityController(userSession);
    }
  }
}
