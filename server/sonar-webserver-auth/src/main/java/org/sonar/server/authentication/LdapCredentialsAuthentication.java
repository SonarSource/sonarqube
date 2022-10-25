/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import org.sonar.api.config.Configuration;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.auth.ldap.LdapAuthenticator;
import org.sonar.auth.ldap.LdapGroupsProvider;
import org.sonar.auth.ldap.LdapRealm;
import org.sonar.auth.ldap.LdapUserDetails;
import org.sonar.auth.ldap.LdapUsersProvider;
import org.sonar.db.user.UserDto;
import org.sonar.process.ProcessProperties;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.user.ExternalIdentity;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

public class LdapCredentialsAuthentication {

  private static final String LDAP_SECURITY_REALM = "LDAP";

  private static final Logger LOG = Loggers.get(LdapCredentialsAuthentication.class);

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

    String realmName = configuration.get(ProcessProperties.Property.SONAR_SECURITY_REALM.getKey()).orElse(null);
    this.isLdapAuthActivated = LDAP_SECURITY_REALM.equals(realmName);

    if (isLdapAuthActivated) {
      ldapRealm.init();
      this.ldapAuthenticator = ldapRealm.doGetAuthenticator();
      this.ldapUsersProvider = ldapRealm.getUsersProvider();
      this.ldapGroupsProvider = ldapRealm.getGroupsProvider();
    } else {
      this.ldapAuthenticator = null;
      this.ldapUsersProvider = null;
      this.ldapGroupsProvider = null;
    }
  }

  public Optional<UserDto> authenticate(Credentials credentials, HttpServletRequest request, AuthenticationEvent.Method method) {
    if (isLdapAuthActivated) {
      return Optional.of(doAuthenticate(fixCase(credentials), request, method));
    }
    return Optional.empty();
  }

  private UserDto doAuthenticate(Credentials credentials, HttpServletRequest request, AuthenticationEvent.Method method) {
    try {
      LdapUsersProvider.Context ldapUsersProviderContext = new LdapUsersProvider.Context(credentials.getLogin(), request);
      LdapUserDetails details = ldapUsersProvider.doGetUserDetails(ldapUsersProviderContext);
      if (details == null) {
        throw AuthenticationException.newBuilder()
          .setSource(realmEventSource(method))
          .setLogin(credentials.getLogin())
          .setMessage("No user details")
          .build();
      }
      LdapAuthenticator.Context ldapAuthenticatorContext = new LdapAuthenticator.Context(credentials.getLogin(), credentials.getPassword().orElse(null), request);
      boolean status = ldapAuthenticator.doAuthenticate(ldapAuthenticatorContext);
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

  private static Source realmEventSource(AuthenticationEvent.Method method) {
    return Source.realm(method, "ldap");
  }

  private UserDto synchronize(String userLogin, LdapUserDetails details, HttpServletRequest request, AuthenticationEvent.Method method) {
    String name = details.getName();
    UserIdentity.Builder userIdentityBuilder = UserIdentity.builder()
      .setName(isEmpty(name) ? userLogin : name)
      .setEmail(trimToNull(details.getEmail()))
      .setProviderLogin(userLogin);
    if (ldapGroupsProvider != null) {
      LdapGroupsProvider.Context context = new LdapGroupsProvider.Context(userLogin, request);
      Collection<String> groups = ldapGroupsProvider.doGetGroups(context);
      userIdentityBuilder.setGroups(new HashSet<>(groups));
    }
    return userRegistrar.register(
      UserRegistration.builder()
        .setUserIdentity(userIdentityBuilder.build())
        .setProvider(new ExternalIdentityProvider())
        .setSource(realmEventSource(method))
        .build());
  }

  private Credentials fixCase(Credentials credentials) {
    if (configuration.getBoolean("sonar.authenticator.downcase").orElse(false)) {
      return new Credentials(credentials.getLogin().toLowerCase(Locale.ENGLISH), credentials.getPassword().orElse(null));
    }
    return credentials;
  }

  private static class ExternalIdentityProvider implements IdentityProvider {
    @Override
    public String getKey() {
      return ExternalIdentity.SQ_AUTHORITY;
    }

    @Override
    public String getName() {
      return ExternalIdentity.SQ_AUTHORITY;
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
