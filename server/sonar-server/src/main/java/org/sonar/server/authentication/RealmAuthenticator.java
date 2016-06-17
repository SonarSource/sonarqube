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

package org.sonar.server.authentication;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.sonar.api.CoreProperties.CORE_AUTHENTICATOR_CREATE_USERS;
import static org.sonar.server.user.UserUpdater.SQ_AUTHORITY;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.sonar.api.Startable;
import org.sonar.api.config.Settings;
import org.sonar.api.security.Authenticator;
import org.sonar.api.security.ExternalGroupsProvider;
import org.sonar.api.security.ExternalUsersProvider;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.security.UserDetails;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.SecurityRealmFactory;

public class RealmAuthenticator implements Startable {

  private final Settings settings;
  private final SecurityRealmFactory securityRealmFactory;
  private final UserIdentityAuthenticator userIdentityAuthenticator;

  private SecurityRealm realm;
  private Authenticator authenticator;
  private ExternalUsersProvider externalUsersProvider;
  private ExternalGroupsProvider externalGroupsProvider;

  public RealmAuthenticator(Settings settings, SecurityRealmFactory securityRealmFactory, UserIdentityAuthenticator userIdentityAuthenticator) {
    this.settings = settings;
    this.securityRealmFactory = securityRealmFactory;
    this.userIdentityAuthenticator = userIdentityAuthenticator;
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

  public boolean isExternalAuthenticationUsed() {
    return realm != null;
  }

  public UserDto authenticate(String userLogin, String userPassword, HttpServletRequest request) {
    failIfNoRealm();
    return doAuthenticate(getLogin(userLogin), userPassword, request);
  }

  private UserDto doAuthenticate(String userLogin, String userPassword, HttpServletRequest request) {
    ExternalUsersProvider.Context externalUsersProviderContext = new ExternalUsersProvider.Context(userLogin, request);
    UserDetails details = externalUsersProvider.doGetUserDetails(externalUsersProviderContext);
    if (details == null) {
      throw new UnauthorizedException("No user details");
    }
    Authenticator.Context authenticatorContext = new Authenticator.Context(userLogin, userPassword, request);
    boolean status = authenticator.doAuthenticate(authenticatorContext);
    if (!status) {
      throw new UnauthorizedException("Fail to authenticate from external provider");
    }
    return synchronize(userLogin, details, request);
  }

  private UserDto synchronize(String userLogin, UserDetails details, HttpServletRequest request) {
    String name = details.getName();
    UserIdentity.Builder userIdentityBuilder = UserIdentity.builder()
      .setLogin(userLogin)
      .setName(name != null ? name : userLogin)
      .setEmail(trimToNull(details.getEmail()))
      .setProviderLogin(userLogin);
    if (externalGroupsProvider != null) {
      ExternalGroupsProvider.Context context = new ExternalGroupsProvider.Context(userLogin, request);
      Collection<String> groups = externalGroupsProvider.doGetGroups(context);
      userIdentityBuilder.setGroups(new HashSet<>(groups));
    }
    return userIdentityAuthenticator.authenticate(userIdentityBuilder.build(), new ExternalIdentityProvider());
  }

  private String getLogin(String userLogin) {
    if (settings.getBoolean("sonar.authenticator.downcase")) {
      return userLogin.toLowerCase(Locale.ENGLISH);
    }
    return userLogin;
  }

  private void failIfNoRealm() {
    checkState(realm != null, "No realm available");
  }

  private class ExternalIdentityProvider implements IdentityProvider {
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
      return settings.getBoolean(CORE_AUTHENTICATOR_CREATE_USERS);
    }
  }

  @Override
  public void stop() {
    // Nothing to do
  }
}
