/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.tester;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * {@code UserSessionRule} is intended to be used as a {@link org.junit.Rule} to easily manage {@link UserSession} in
 * unit tests.
 * <p>
 * It can be used as a {@link org.junit.ClassRule} but be careful not to modify its states from inside tests methods
 * unless you purposely want to have side effects between each tests.
 * </p>
 * <p>
 * {@code UserSessionRule} is intended to be used either standalone (in which case use the static factory method
 * {@link #standalone()} or with {@link ServerTester} (in which case use static factory method
 * {@link #forServerTester(ServerTester)}).
 * </p>
 * <p>
 * In both cases, one can define user session behavior which should apply on all tests directly on the property, eg.:
 * <pre>
 * {@literal @}Rule
 * public UserSessionRule userSessionRule = UserSessionRule.standalone().login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
 * </pre>
 * </p>
 * <p>
 * Behavior defined at property-level can obviously be override at test method level. For example, one could define
 * all methods to use an authenticated session such as presented above but can easily overwrite that behavior in a
 * specific test method as follow:
 * <pre>
 * {@literal @}Test
 * public void test_method() {
 *   userSessionRule.standalone();
 *   {@literal [...]}
 * }
 * </pre>
 * </p>
 * <p>
 * {@code UserSessionRule}, being standalone or associated to a {@link ServerTester}, emulates by default an anonymous
 * session. Therefore, call {@code UserSessionRule.standalone()} is equivalent to calling
 * {@code UserSessionRule.standalone().anonymous()}.
 * </p>
 * <p>
 * To emulate an identified user, either use method {@link #login(String)} if you want to specify the user's login, or
 * method {@link #login()} which will do the same but using the value of {@link #DEFAULT_LOGIN} as the user's login
 * (use the latest override if you don't care about the actual value of the login, it will save noise in your test).
 * </p>
 */
public class UserSessionRule implements TestRule, UserSession {
  public static final String DEFAULT_LOGIN = "default_login";

  @CheckForNull
  private final ServerTester serverTester;
  private UserSession currentUserSession;

  private UserSessionRule(@Nullable ServerTester serverTester) {
    this.serverTester = serverTester;
    anonymous();
  }

  public static UserSessionRule standalone() {
    return new UserSessionRule(null);
  }

  public static UserSessionRule forServerTester(ServerTester serverTester) {
    return new UserSessionRule(Preconditions.checkNotNull(serverTester));
  }

  /**
   * Log in with the default login {@link #DEFAULT_LOGIN}
   */
  public UserSessionRule login() {
    return login(DEFAULT_LOGIN);
  }

  /**
   * Log in with the specified login
   */
  public UserSessionRule login(String login) {
    setCurrentUserSession(new MockUserSession(login));
    return this;
  }

  /**
   * Disconnect/go anonymous
   */
  public UserSessionRule anonymous() {
    setCurrentUserSession(new AnonymousMockUserSession());
    return this;
  }

  @Override
  public Statement apply(Statement statement, Description description) {
    return this.statement(statement);
  }

  private Statement statement(final Statement base) {
    return new Statement() {
      public void evaluate() throws Throwable {
        UserSessionRule.this.before();

        try {
          base.evaluate();
        } finally {
          UserSessionRule.this.after();
        }

      }
    };
  }

  protected void before() throws Throwable {
    setCurrentUserSession(currentUserSession);
  }

  protected void after() {
    this.currentUserSession = null;
    if (serverTester != null) {
      serverTester.get(ThreadLocalUserSession.class).remove();
    }
  }

  public void set(UserSession userSession) {
    checkState(serverTester == null, "Can set a specific session and use ServerTester at the same time");
    checkNotNull(userSession);
    setCurrentUserSession(userSession);
  }

  public UserSessionRule setGlobalPermissions(String... globalPermissions) {
    ensureMockUserSession().setGlobalPermissions(globalPermissions);
    return this;
  }

  public UserSessionRule addProjectUuidPermissions(String projectPermission, String... projectUuids) {
    ensureMockUserSession().addProjectUuidPermissions(projectPermission, projectUuids);
    return this;
  }

  @Deprecated
  public UserSessionRule addComponentPermission(String projectPermission, String projectKey, String componentKey) {
    ensureMockUserSession().addComponentPermission(projectPermission, projectKey, componentKey);
    return this;
  }

  @Deprecated
  public UserSessionRule addProjectPermissions(String projectPermission, String... projectKeys) {
    ensureMockUserSession().addProjectPermissions(projectPermission, projectKeys);
    return this;
  }

  public UserSessionRule setUserId(@Nullable Integer userId) {
    ensureMockUserSession().setUserId(userId);
    return this;
  }

  public UserSessionRule setUserGroups(@Nullable String... userGroups) {
    ensureMockUserSession().setUserGroups(userGroups);
    return this;
  }

  public UserSessionRule setLocale(@Nullable Locale l) {
    ensureMockUserSession().setLocale(l);
    return this;
  }

  public UserSessionRule addComponentUuidPermission(String projectPermission, String projectUuid, String componentUuid) {
    ensureMockUserSession().addComponentUuidPermission(projectPermission, projectUuid, componentUuid);
    return this;
  }

  public UserSessionRule setName(@Nullable String s) {
    ensureMockUserSession().setName(s);
    return this;
  }

  private MockUserSession ensureMockUserSession() {
    checkState(currentUserSession instanceof MockUserSession, "rule state can not be changed if a UserSession has explicitly been provided");
    return (MockUserSession) currentUserSession;
  }

  private void setCurrentUserSession(UserSession userSession) {
    this.currentUserSession = Preconditions.checkNotNull(userSession);
    if (serverTester != null) {
      serverTester.get(ThreadLocalUserSession.class).set(currentUserSession);
    }
  }

  @Override
  public List<String> globalPermissions() {
    return currentUserSession.globalPermissions();
  }

  @Override
  public boolean hasComponentPermission(String permission, String componentKey) {
    return currentUserSession.hasComponentPermission(permission, componentKey);
  }

  @Override
  public boolean hasComponentUuidPermission(String permission, String componentUuid) {
    return currentUserSession.hasComponentUuidPermission(permission, componentUuid);
  }

  @Override
  @CheckForNull
  public String getLogin() {
    return currentUserSession.getLogin();
  }

  @Override
  @CheckForNull
  public String getName() {
    return currentUserSession.getName();
  }

  @Override
  @CheckForNull
  public Integer getUserId() {
    return currentUserSession.getUserId();
  }

  @Override
  public Set<String> getUserGroups() {
    return currentUserSession.getUserGroups();
  }

  @Override
  public boolean isLoggedIn() {
    return currentUserSession.isLoggedIn();
  }

  @Override
  public Locale locale() {
    return currentUserSession.locale();
  }

  @Override
  public UserSession checkLoggedIn() {
    return currentUserSession.checkLoggedIn();
  }

  @Override
  public UserSession checkPermission(String globalPermission) {
    return currentUserSession.checkPermission(globalPermission);
  }

  @Override
  public UserSession checkGlobalPermission(String globalPermission) {
    return currentUserSession.checkGlobalPermission(globalPermission);
  }

  @Override
  public UserSession checkAnyPermissions(Collection<String> globalPermissions) {
    return currentUserSession.checkAnyPermissions(globalPermissions);
  }

  @Override
  public boolean hasPermission(String globalPermission) {
    return currentUserSession.hasPermission(globalPermission);
  }

  @Override
  public boolean hasGlobalPermission(String globalPermission) {
    return currentUserSession.hasGlobalPermission(globalPermission);
  }

  @Override
  public UserSession checkComponentPermission(String projectPermission, String componentKey) {
    return currentUserSession.checkComponentPermission(projectPermission, componentKey);
  }

  @Override
  public UserSession checkComponentUuidPermission(String permission, String componentUuid) {
    return currentUserSession.checkComponentUuidPermission(permission, componentUuid);
  }


}
