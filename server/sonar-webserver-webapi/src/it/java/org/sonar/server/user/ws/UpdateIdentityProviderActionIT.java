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
package org.sonar.server.user.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.authentication.IdentityProviderRepositoryRule;
import org.sonar.server.authentication.TestIdentityProvider;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UpdateIdentityProviderActionIT {
  private final static String SQ_AUTHORITY = "sonarqube";

  @Rule
  public IdentityProviderRepositoryRule identityProviderRepository = new IdentityProviderRepositoryRule()
    .addIdentityProvider(new TestIdentityProvider().setName("Gitlab").setKey("gitlab").setEnabled(true))
    .addIdentityProvider(new TestIdentityProvider().setName("Github").setKey("github").setEnabled(true));

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setSystemAdministrator();

  private final MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", "1");
  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(dbClient, settings.asConfig());

  private final ManagedInstanceChecker managedInstanceChecker = mock(ManagedInstanceChecker.class);

  private final WsActionTester underTest = new WsActionTester(new UpdateIdentityProviderAction(dbClient, identityProviderRepository,
    new UserUpdater(mock(NewUserNotifier.class), dbClient, new DefaultGroupFinder(db.getDbClient()), settings.asConfig(), null, localAuthentication),
    userSession, managedInstanceChecker));

  @Test
  public void change_identity_provider_of_a_local_user_all_params() {
    String userLogin = "login-1";
    String newExternalLogin = "login@github.com";
    String newExternalIdentityProvider = "github";
    createUser(true, userLogin, userLogin, SQ_AUTHORITY);
    TestRequest request = underTest.newRequest()
      .setParam("login", userLogin)
      .setParam("newExternalProvider", newExternalIdentityProvider)
      .setParam("newExternalIdentity", newExternalLogin);

    request.execute();
    assertThat(dbClient.userDao().selectByExternalLoginAndIdentityProvider(dbSession, newExternalLogin, newExternalIdentityProvider))
      .isNotNull()
      .extracting(UserDto::isLocal, UserDto::getExternalLogin, UserDto::getExternalIdentityProvider)
      .contains(false, newExternalLogin, newExternalIdentityProvider);
  }

  @Test
  public void change_identity_provider_of_a_local_user_mandatory_params_only_provider_login_stays_same() {
    String userLogin = "login-1";
    String newExternalIdentityProvider = "github";
    createUser(true, userLogin, userLogin, SQ_AUTHORITY);
    TestRequest request = underTest.newRequest()
      .setParam("login", userLogin)
      .setParam("newExternalProvider", newExternalIdentityProvider);

    request.execute();
    assertThat(dbClient.userDao().selectByExternalLoginAndIdentityProvider(dbSession, userLogin, newExternalIdentityProvider))
      .isNotNull()
      .extracting(UserDto::isLocal, UserDto::getExternalLogin, UserDto::getExternalIdentityProvider)
      .contains(false, userLogin, newExternalIdentityProvider);
  }

  @Test
  public void change_identity_provider_of_a_external_user_to_new_one() {
    String userLogin = "login-1";
    String oldExternalIdentityProvider = "gitlab";
    String oldExternalIdentity = "john@gitlab.com";
    createUser(false, userLogin, oldExternalIdentity, oldExternalIdentityProvider);

    String newExternalIdentityProvider = "github";
    String newExternalIdentity = "john@github.com";
    TestRequest request = underTest.newRequest()
      .setParam("login", userLogin)
      .setParam("newExternalProvider", newExternalIdentityProvider)
      .setParam("newExternalIdentity", newExternalIdentity);

    request.execute();
    assertThat(dbClient.userDao().selectByExternalLoginAndIdentityProvider(dbSession, newExternalIdentity, newExternalIdentityProvider))
      .isNotNull()
      .extracting(UserDto::isLocal, UserDto::getExternalLogin, UserDto::getExternalIdentityProvider)
      .contains(false, newExternalIdentity, newExternalIdentityProvider);
  }

  @Test
  public void change_identity_provider_of_a_local_user_to_ldap_default_using_sonarqube_as_parameter() {
    String userLogin = "login-1";
    String newExternalIdentityProvider = "LDAP_default";
    createUser(true, userLogin, userLogin, SQ_AUTHORITY);
    TestRequest request = underTest.newRequest()
      .setParam("login", userLogin)
      .setParam("newExternalProvider", "sonarqube");

    request.execute();
    assertThat(dbClient.userDao().selectByExternalLoginAndIdentityProvider(dbSession, userLogin, newExternalIdentityProvider))
      .isNotNull()
      .extracting(UserDto::isLocal, UserDto::getExternalLogin, UserDto::getExternalIdentityProvider)
      .contains(false, userLogin, newExternalIdentityProvider);
  }

  @Test
  public void change_identity_provider_of_a_local_user_to_ldap_default_using_ldap_as_parameter() {
    String userLogin = "login-1";
    String newExternalIdentityProvider = "LDAP_default";
    createUser(true, userLogin, userLogin, SQ_AUTHORITY);
    TestRequest request = underTest.newRequest()
      .setParam("login", userLogin)
      .setParam("newExternalProvider", "LDAP");

    request.execute();
    assertThat(dbClient.userDao().selectByExternalLoginAndIdentityProvider(dbSession, userLogin, newExternalIdentityProvider))
      .isNotNull()
      .extracting(UserDto::isLocal, UserDto::getExternalLogin, UserDto::getExternalIdentityProvider)
      .contains(false, userLogin, newExternalIdentityProvider);
  }

  @Test
  public void change_identity_provider_of_a_local_user_to_ldap_default_using_ldap_server_key_as_parameter() {
    String userLogin = "login-1";
    String newExternalIdentityProvider = "LDAP_server42";
    createUser(true, userLogin, userLogin, SQ_AUTHORITY);
    TestRequest request = underTest.newRequest()
      .setParam("login", userLogin)
      .setParam("newExternalProvider", newExternalIdentityProvider);

    request.execute();
    assertThat(dbClient.userDao().selectByExternalLoginAndIdentityProvider(dbSession, userLogin, newExternalIdentityProvider))
      .isNotNull()
      .extracting(UserDto::isLocal, UserDto::getExternalLogin, UserDto::getExternalIdentityProvider)
      .contains(false, userLogin, newExternalIdentityProvider);
  }

  @Test
  public void fail_if_user_not_exist() {
    TestRequest request = underTest.newRequest()
      .setParam("login", "not-existing")
      .setParam("newExternalProvider", "gitlab");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User 'not-existing' doesn't exist");
  }

  @Test
  public void fail_if_identity_provider_not_exist() {
    createUser(true, "login-1", "login-1", SQ_AUTHORITY);
    TestRequest request = underTest.newRequest()
      .setParam("login", "login-1")
      .setParam("newExternalProvider", "not-existing");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'newExternalProvider' (not-existing) must be one of: [github, gitlab] or [LDAP, LDAP_{serverKey}]");
  }

  @Test
  public void fail_if_anonymous() {
    userSession.anonymous();
    TestRequest request = underTest.newRequest()
      .setParam("login", "not-existing")
      .setParam("newExternalProvider", "something");

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_if_not_admin() {
    userSession.logIn("some-user");
    TestRequest request = underTest.newRequest()
      .setParam("login", "not-existing")
      .setParam("newExternalProvider", "something");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void handle_whenInstanceManaged_shouldThrowBadRequestException() {
    BadRequestException badRequestException = BadRequestException.create("message");
    doThrow(badRequestException).when(managedInstanceChecker).throwIfInstanceIsManaged();

    TestRequest request = underTest.newRequest();

    assertThatThrownBy(request::execute)
      .isEqualTo(badRequestException);
  }

  @Test
  public void handle_whenInstanceManagedAndNotSystemAdministrator_shouldThrowUnauthorizedException() {
    userSession.anonymous();

    TestRequest request = underTest.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
    verify(managedInstanceChecker, never()).throwIfInstanceIsManaged();
  }

  private void createUser(boolean local, String login, String externalLogin, String externalIdentityProvider) {
    UserDto userDto = newUserDto()
      .setEmail("john@email.com")
      .setLogin(login)
      .setName("John")
      .setScmAccounts(newArrayList("jn"))
      .setActive(true)
      .setLocal(local)
      .setExternalLogin(externalLogin)
      .setExternalIdentityProvider(externalIdentityProvider);
    dbClient.userDao().insert(dbSession, userDto);
    dbSession.commit();
  }

}
