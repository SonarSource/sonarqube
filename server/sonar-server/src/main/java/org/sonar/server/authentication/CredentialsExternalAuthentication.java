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
import org.sonar.server.authentication.UserRegistration.ExistingEmailStrategy;
import org.sonar.server.authentication.UserRegistration.UpdateLoginStrategy;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.user.SecurityRealmFactory;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.sonar.server.user.ExternalIdentity.SQ_AUTHORITY;

/**
 * Delegates the validation of credentials to an external system, e.g. LDAP.
 */
public class CredentialsExternalAuthentication implements Startable {

  private static final Logger LOG = Loggers.get(CredentialsExternalAuthentication.class);

  private final Configuration config;
  private final SecurityRealmFactory securityRealmFactory;
  private final UserRegistrar userRegistrar;
  private final AuthenticationEvent authenticationEvent;

  private SecurityRealm realm;
  private Authenticator authenticator;
  private ExternalUsersProvider externalUsersProvider;
  private ExternalGroupsProvider externalGroupsProvider;

  public CredentialsExternalAuthentication(Configuration config, SecurityRealmFactory securityRealmFactory,
    UserRegistrar userRegistrar, AuthenticationEvent authenticationEvent) {
    this.config = config;
    this.securityRealmFactory = securityRealmFactory;
    this.userRegistrar = userRegistrar;
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

  public Optional<UserDto> authenticate(Credentials credentials, HttpServletRequest request, AuthenticationEvent.Method method) {
    if (realm == null) {
      return Optional.empty();
    }
    return Optional.of(doAuthenticate(fixCase(credentials), request, method));
  }

  private UserDto doAuthenticate(Credentials credentials, HttpServletRequest request, AuthenticationEvent.Method method) {
    try {
      ExternalUsersProvider.Context externalUsersProviderContext = new ExternalUsersProvider.Context(credentials.getLogin(), request);
      UserDetails details = externalUsersProvider.doGetUserDetails(externalUsersProviderContext);
      if (details == null) {
        throw AuthenticationException.newBuilder()
          .setSource(realmEventSource(method))
          .setLogin(credentials.getLogin())
          .setMessage("No user details")
          .build();
      }
      Authenticator.Context authenticatorContext = new Authenticator.Context(credentials.getLogin(), credentials.getPassword().orElse(null), request);
      boolean status = authenticator.doAuthenticate(authenticatorContext);
      if (!status) {
        throw AuthenticationException.newBuilder()
          .setSource(realmEventSource(method))
          .setLogin(credentials.getLogin())
          .setMessage("Realm returned authenticate=false")
          .build();
      }
      UserDto userDto = synchronize(credentials.getLogin(), details, request, method);
      authenticationEvent.loginSuccess(request, credentials.getLogin(), realmEventSource(method));
      return userDto;
    } catch (AuthenticationException e) {
      throw e;
    } catch (Exception e) {
      // It seems that with Realm API it's expected to log the error and to not authenticate the user
      LOG.error("Error during authentication", e);
      throw AuthenticationException.newBuilder()
        .setSource(realmEventSource(method))
        .setLogin(credentials.getLogin())
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
    return userRegistrar.register(
      UserRegistration.builder()
        .setUserIdentity(userIdentityBuilder.build())
        .setProvider(new ExternalIdentityProvider())
        .setSource(realmEventSource(method))
        .setExistingEmailStrategy(ExistingEmailStrategy.FORBID)
        .setUpdateLoginStrategy(UpdateLoginStrategy.ALLOW)
        .build());
  }

  private Credentials fixCase(Credentials credentials) {
    if (config.getBoolean("sonar.authenticator.downcase").orElse(false)) {
      return new Credentials(credentials.getLogin().toLowerCase(Locale.ENGLISH), credentials.getPassword().orElse(null));
    }
    return credentials;
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
