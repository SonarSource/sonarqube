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
package org.sonar.server.authentication;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.auth.ldap.LdapAuthenticationResult;
import org.sonar.auth.ldap.LdapAuthenticator;
import org.sonar.auth.ldap.LdapGroupsProvider;
import org.sonar.auth.ldap.LdapRealm;
import org.sonar.auth.ldap.LdapUserDetails;
import org.sonar.auth.ldap.LdapUsersProvider;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.authentication.event.AuthenticationException;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class LdapCredentialsAuthentication {

  private static final Logger LOG = LoggerFactory.getLogger(LdapCredentialsAuthentication.class);

  private final Configuration configuration;
  private final UserRegistrar userRegistrar;
  private final AuthenticationEvent authenticationEvent;

  private final LdapAuthenticator ldapAuthenticator;
  private final LdapUsersProvider ldapUsersProvider;
  private final LdapGroupsProvider ldapGroupsProvider;
  private final boolean isLdapAuthActivated;

  public LdapCredentialsAuthentication(Configuration configuration,
    UserRegistrar userRegistrar, AuthenticationEvent authenticationEvent, LdapRealm ldapRealm) {
    this.configuration = configuration;
    this.userRegistrar = userRegistrar;
    this.authenticationEvent = authenticationEvent;

    this.isLdapAuthActivated = ldapRealm.isLdapAuthActivated();
    this.ldapAuthenticator = ldapRealm.getAuthenticator();
    this.ldapUsersProvider = ldapRealm.getUsersProvider();
    this.ldapGroupsProvider = ldapRealm.getGroupsProvider();
  }

  public Optional<UserDto> authenticate(Credentials credentials, HttpRequest request, AuthenticationEvent.Method method) {
    if (isLdapAuthActivated) {
      return Optional.of(doAuthenticate(fixCase(credentials), request, method));
    }
    return Optional.empty();
  }

  private UserDto doAuthenticate(Credentials credentials, HttpRequest request, AuthenticationEvent.Method method) {
    try {
      LdapAuthenticator.Context ldapAuthenticatorContext = new LdapAuthenticator.Context(credentials.getLogin(), credentials.getPassword().orElse(null), request);
      LdapAuthenticationResult authenticationResult = ldapAuthenticator.doAuthenticate(ldapAuthenticatorContext);
      if (!authenticationResult.isSuccess()) {
        throw AuthenticationException.newBuilder()
          .setSource(realmEventSource(method))
          .setLogin(credentials.getLogin())
          .setMessage("Realm returned authenticate=false")
          .build();
      }

      LdapUsersProvider.Context ldapUsersProviderContext = new LdapUsersProvider.Context(authenticationResult.getServerKey(), credentials.getLogin(), request);
      LdapUserDetails ldapUserDetails = ldapUsersProvider.doGetUserDetails(ldapUsersProviderContext);
      if (ldapUserDetails == null) {
        throw AuthenticationException.newBuilder()
          .setSource(realmEventSource(method))
          .setLogin(credentials.getLogin())
          .setMessage("No user details")
          .build();
      }
      UserDto userDto = synchronize(credentials.getLogin(), authenticationResult.getServerKey(), ldapUserDetails, request, method);
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

  private static Source realmEventSource(AuthenticationEvent.Method method) {
    return Source.realm(method, "ldap");
  }

  private UserDto synchronize(String userLogin, String serverKey, LdapUserDetails userDetails, HttpRequest request, AuthenticationEvent.Method method) {
    String name = userDetails.getName();
    UserIdentity.Builder userIdentityBuilder = UserIdentity.builder()
      .setName(isEmpty(name) ? userLogin : name)
      .setEmail(trimToNull(userDetails.getEmail()))
      .setProviderLogin(userLogin);
    if (ldapGroupsProvider != null) {
      LdapGroupsProvider.Context context = new LdapGroupsProvider.Context(serverKey, userLogin, request);
      Collection<String> groups = ldapGroupsProvider.doGetGroups(context);
      userIdentityBuilder.setGroups(new HashSet<>(groups));
    }
    return userRegistrar.register(
      UserRegistration.builder()
        .setUserIdentity(userIdentityBuilder.build())
        .setProvider(new LdapIdentityProvider(serverKey))
        .setSource(realmEventSource(method))
        .build());
  }

  private Credentials fixCase(Credentials credentials) {
    if (configuration.getBoolean("sonar.authenticator.downcase").orElse(false)) {
      return new Credentials(credentials.getLogin().toLowerCase(Locale.ENGLISH), credentials.getPassword().orElse(null));
    }
    return credentials;
  }

  private static class LdapIdentityProvider implements IdentityProvider {

    private final String key;

    private LdapIdentityProvider(String ldapServerKey) {
      this.key = LdapRealm.LDAP_SECURITY_REALM + "_" + ldapServerKey;
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public String getName() {
      return getKey();
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

}
