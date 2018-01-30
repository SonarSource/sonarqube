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
package org.sonar.server.authentication;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.security.Authenticator;
import org.sonar.api.security.ExternalGroupsProvider;
import org.sonar.api.security.ExternalUsersProvider;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.security.UserDetails;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.user.SecurityRealmFactory;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.sonar.server.authentication.UserIdentityAuthenticator.ExistingEmailStrategy.FORBID;
import static org.sonar.server.user.ExternalIdentity.SQ_AUTHORITY;

public class RealmAuthenticator implements Startable {

  private static final Logger LOG = Loggers.get(RealmAuthenticator.class);

  private final Configuration config;
  private final SecurityRealmFactory securityRealmFactory;
  private final UserIdentityAuthenticator userIdentityAuthenticator;
  private final AuthenticationEvent authenticationEvent;

  private SecurityRealm realm;
  private Authenticator authenticator;
  private ExternalUsersProvider externalUsersProvider;
  private ExternalGroupsProvider externalGroupsProvider;

  public RealmAuthenticator(Configuration config, SecurityRealmFactory securityRealmFactory,
    UserIdentityAuthenticator userIdentityAuthenticator, AuthenticationEvent authenticationEvent) {
    this.config = config;
    this.securityRealmFactory = securityRealmFactory;
    this.userIdentityAuthenticator = userIdentityAuthenticator;
    this.authenticationEvent = authenticationEvent;
  }

  @Override
  public void start() {
    realm = securityRealmFactory.getRealm();
    if (realm != null) {
      authenticator = requireNonNull(realm.doGetAuthenticator(), "No authenticator available");
      externalUsersProvider = requireNonNull(realm.getUsersProvider(), "No users provider available");
      externalGroupsProvider = realm.getGroupsProvider();
    }
  }

  public Optional<UserDto> authenticate(String userLogin, String userPassword, HttpServletRequest request, AuthenticationEvent.Method method) {
    if (realm == null) {
      return Optional.empty();
    }
    return Optional.of(doAuthenticate(getLogin(userLogin), userPassword, request, method));
  }

  private UserDto doAuthenticate(String userLogin, String userPassword, HttpServletRequest request, AuthenticationEvent.Method method) {
    try {
      ExternalUsersProvider.Context externalUsersProviderContext = new ExternalUsersProvider.Context(userLogin, request);
      UserDetails details = externalUsersProvider.doGetUserDetails(externalUsersProviderContext);
      if (details == null) {
        throw AuthenticationException.newBuilder()
          .setSource(realmEventSource(method))
          .setLogin(userLogin)
          .setMessage("No user details")
          .build();
      }
      Authenticator.Context authenticatorContext = new Authenticator.Context(userLogin, userPassword, request);
      boolean status = authenticator.doAuthenticate(authenticatorContext);
      if (!status) {
        throw AuthenticationException.newBuilder()
          .setSource(realmEventSource(method))
          .setLogin(userLogin)
          .setMessage("Realm returned authenticate=false")
          .build();
      }
      UserDto userDto = synchronize(userLogin, details, request, method);
      authenticationEvent.loginSuccess(request, userLogin, realmEventSource(method));
      return userDto;
    } catch (AuthenticationException e) {
      throw e;
    } catch (Exception e) {
      // It seems that with Realm API it's expected to log the error and to not authenticate the user
      LOG.error("Error during authentication", e);
      throw AuthenticationException.newBuilder()
        .setSource(realmEventSource(method))
        .setLogin(userLogin)
        .setMessage(e.getMessage())
        .build();
    }
  }

  private Source realmEventSource(AuthenticationEvent.Method method) {
    return Source.realm(method, realm.getName());
  }

  private UserDto synchronize(String userLogin, UserDetails details, HttpServletRequest request, AuthenticationEvent.Method method) {
    String name = details.getName();
    UserIdentity.Builder userIdentityBuilder = UserIdentity.builder()
      .setLogin(userLogin)
      .setName(isEmpty(name) ? userLogin : name)
      .setEmail(trimToNull(details.getEmail()))
      .setProviderLogin(userLogin);
    if (externalGroupsProvider != null) {
      ExternalGroupsProvider.Context context = new ExternalGroupsProvider.Context(userLogin, request);
      Collection<String> groups = externalGroupsProvider.doGetGroups(context);
      userIdentityBuilder.setGroups(new HashSet<>(groups));
    }
    return userIdentityAuthenticator.authenticate(userIdentityBuilder.build(), new ExternalIdentityProvider(), realmEventSource(method), FORBID);
  }

  private String getLogin(String userLogin) {
    if (config.getBoolean("sonar.authenticator.downcase").orElse(false)) {
      return userLogin.toLowerCase(Locale.ENGLISH);
    }
    return userLogin;
  }

  private static class ExternalIdentityProvider implements IdentityProvider {
    @Override
    public String getKey() {
      return SQ_AUTHORITY;
    }

    @Override
    public String getName() {
      return SQ_AUTHORITY;
    }

    @Override
    public Display getDisplay() {
      return null;
    }

    @Override
    public boolean isEnabled() {
      return true;
    }

    @Override
    public boolean allowsUsersToSignUp() {
      return true;
    }
  }

  @Override
  public void stop() {
    // Nothing to do
  }
}
