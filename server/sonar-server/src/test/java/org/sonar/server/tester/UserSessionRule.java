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
package org.sonar.server.tester;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
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
 * One can define user session behavior which should apply on all tests directly on the property, eg.:
 * <pre>
 * {@literal @}Rule
 * public UserSessionRule userSession = UserSessionRule.standalone().login("admin").setOrganizationPermissions(OrganizationPermissions.SYSTEM_ADMIN);
 * </pre>
 * </p>
 * <p>
 * Behavior defined at property-level can obviously be override at test method level. For example, one could define
 * all methods to use an authenticated session such as presented above but can easily overwrite that behavior in a
 * specific test method as follow:
 * <pre>
 * {@literal @}Test
 * public void test_method() {
 *   userSession.standalone();
 *   {@literal [...]}
 * }
 * </pre>
 * </p>
 * <p>
 * {@code UserSessionRule}, emulates by default an anonymous
 * session. Therefore, call {@code UserSessionRule.standalone()} is equivalent to calling
 * {@code UserSessionRule.standalone().anonymous()}.
 * </p>
 * <p>
 * To emulate an identified user, either use method {@link #logIn(String)} if you want to specify the user's login, or
 * method {@link #logIn()} which will do the same but using the value of {@link #DEFAULT_LOGIN} as the user's login
 * (use the latest override if you don't care about the actual value of the login, it will save noise in your test).
 * </p>
 */
public class UserSessionRule implements TestRule, UserSession {
  private static final String DEFAULT_LOGIN = "default_login";

  private UserSession currentUserSession;

  private UserSessionRule() {
    anonymous();
  }

  public static UserSessionRule standalone() {
    return new UserSessionRule();
  }

  /**
   * Log in with the default login {@link #DEFAULT_LOGIN}
   */
  public UserSessionRule logIn() {
    return logIn(DEFAULT_LOGIN);
  }

  /**
   * Log in with the specified login
   */
  public UserSessionRule logIn(String login) {
    setCurrentUserSession(new MockUserSession(login));
    return this;
  }

  /**
   * Log in with the specified login
   */
  public UserSessionRule logIn(UserDto userDto) {
    setCurrentUserSession(new MockUserSession(userDto));
    return this;
  }

  /**
   * Disconnect/go anonymous
   */
  public UserSessionRule anonymous() {
    setCurrentUserSession(new AnonymousMockUserSession());
    return this;
  }

  public UserSessionRule setRoot() {
    ensureMockUserSession().setRoot(true);
    return this;
  }

  public UserSessionRule setNonRoot() {
    ensureMockUserSession().setRoot(false);
    return this;
  }

  public UserSessionRule setSystemAdministrator() {
    ensureMockUserSession().setSystemAdministrator(true);
    return this;
  }

  public UserSessionRule setNonSystemAdministrator() {
    ensureMockUserSession().setSystemAdministrator(false);
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

  protected void before() {
    setCurrentUserSession(currentUserSession);
  }

  protected void after() {
    this.currentUserSession = null;
  }

  public void set(UserSession userSession) {
    checkNotNull(userSession);
    setCurrentUserSession(userSession);
  }

  public UserSessionRule registerComponents(ComponentDto... componentDtos) {
    ensureAbstractMockUserSession().registerComponents(componentDtos);
    return this;
  }

  public UserSessionRule addProjectPermission(String projectPermission, ComponentDto... components) {
    ensureAbstractMockUserSession().addProjectPermission(projectPermission, components);
    return this;
  }

  public UserSessionRule addPermission(OrganizationPermission permission, String organizationUuid) {
    ensureAbstractMockUserSession().addPermission(permission, organizationUuid);
    return this;
  }

  public UserSessionRule addPermission(OrganizationPermission permission, OrganizationDto organization) {
    ensureAbstractMockUserSession().addPermission(permission, organization.getUuid());
    return this;
  }

  public UserSessionRule setUserId(@Nullable Integer userId) {
    ensureMockUserSession().setUserId(userId);
    return this;
  }

  /**
   * Groups that user is member of. User must be logged in. An exception
   * is thrown if session is anonymous.
   */
  public UserSessionRule setGroups(GroupDto... groups) {
    ensureMockUserSession().setGroups(groups);
    return this;
  }

  public UserSessionRule setName(@Nullable String s) {
    ensureMockUserSession().setName(s);
    return this;
  }

  private AbstractMockUserSession ensureAbstractMockUserSession() {
    checkState(currentUserSession instanceof AbstractMockUserSession, "rule state can not be changed if a UserSession has explicitly been provided");
    return (AbstractMockUserSession) currentUserSession;
  }

  private MockUserSession ensureMockUserSession() {
    checkState(currentUserSession instanceof MockUserSession, "rule state can not be changed if a UserSession has explicitly been provided");
    return (MockUserSession) currentUserSession;
  }

  private void setCurrentUserSession(UserSession userSession) {
    this.currentUserSession = Preconditions.checkNotNull(userSession);
  }

  @Override
  public boolean hasComponentPermission(String permission, ComponentDto component) {
    return currentUserSession.hasComponentPermission(permission, component);
  }

  @Override
  public boolean hasComponentUuidPermission(String permission, String componentUuid) {
    return currentUserSession.hasComponentUuidPermission(permission, componentUuid);
  }

  @Override
  public List<ComponentDto> keepAuthorizedComponents(String permission, Collection<ComponentDto> components) {
    return currentUserSession.keepAuthorizedComponents(permission, components);
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
  public Collection<GroupDto> getGroups() {
    return currentUserSession.getGroups();
  }

  @Override
  public boolean isLoggedIn() {
    return currentUserSession.isLoggedIn();
  }

  @Override
  public boolean isRoot() {
    return currentUserSession.isRoot();
  }

  @Override
  public UserSession checkIsRoot() {
    return currentUserSession.checkIsRoot();
  }

  @Override
  public UserSession checkLoggedIn() {
    currentUserSession.checkLoggedIn();
    return this;
  }

  @Override
  public boolean hasPermission(OrganizationPermission permission, OrganizationDto organization) {
    return currentUserSession.hasPermission(permission, organization);
  }

  @Override
  public boolean hasPermission(OrganizationPermission permission, String organizationUuid) {
    return currentUserSession.hasPermission(permission, organizationUuid);
  }

  @Override
  public UserSession checkPermission(OrganizationPermission permission, OrganizationDto organization) {
    currentUserSession.checkPermission(permission, organization);
    return this;
  }

  @Override
  public UserSession checkPermission(OrganizationPermission permission, String organizationUuid) {
    currentUserSession.checkPermission(permission, organizationUuid);
    return this;
  }

  @Override
  public UserSession checkComponentPermission(String projectPermission, ComponentDto component) {
    currentUserSession.checkComponentPermission(projectPermission, component);
    return this;
  }

  @Override
  public UserSession checkComponentUuidPermission(String permission, String componentUuid) {
    currentUserSession.checkComponentUuidPermission(permission, componentUuid);
    return this;
  }

  @Override
  public boolean isSystemAdministrator() {
    return currentUserSession.isSystemAdministrator();
  }

  @Override
  public UserSession checkIsSystemAdministrator() {
    currentUserSession.checkIsSystemAdministrator();
    return this;
  }
}
