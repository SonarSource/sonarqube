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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.config.Settings;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.db.user.UserDto;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.sonar.server.user.UserUpdater.SQ_AUTHORITY;

public class SsoAuthenticator {

  private static final Splitter COMA_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

  private static final String ENABLE_PARAM = "sonar.sso.enable";

  private static final String LOGIN_HEADER_PARAM = "sonar.sso.loginHeader";
  private static final String LOGIN_HEADER_DEFAULT_VALUE = "X-Forwarded-Login";

  private static final String NAME_HEADER_PARAM = "sonar.sso.nameHeader";
  private static final String NAME_HEADER_DEFAULT_VALUE = "X-Forwarded-Name";

  private static final String EMAIL_HEADER_PARAM = "sonar.sso.emailHeader";
  private static final String EMAIL_HEADER_DEFAULT_VALUE = "X-Forwarded-Email";

  private static final String GROUPS_HEADER_PARAM = "sonar.sso.groupsHeader";
  private static final String GROUPS_HEADER_DEFAULT_VALUE = "X-Forwarded-Groups";

  private static final String REFRESH_INTERVAL_PARAM = "sonar.sso.refreshIntervalInMinutes";
  private static final String REFRESH_INTERVAL_DEFAULT_VALUE = "5";

  private static final Map<String, String> DEFAULT_VALUES_BY_PARAMETERS = ImmutableMap.of(
    LOGIN_HEADER_PARAM, LOGIN_HEADER_DEFAULT_VALUE,
    NAME_HEADER_PARAM, NAME_HEADER_DEFAULT_VALUE,
    EMAIL_HEADER_PARAM, EMAIL_HEADER_DEFAULT_VALUE,
    GROUPS_HEADER_PARAM, GROUPS_HEADER_DEFAULT_VALUE,
    REFRESH_INTERVAL_PARAM, REFRESH_INTERVAL_DEFAULT_VALUE);

  private final Settings settings;
  private final UserIdentityAuthenticator userIdentityAuthenticator;
  private final JwtHttpHandler jwtHttpHandler;

  public SsoAuthenticator(Settings settings, UserIdentityAuthenticator userIdentityAuthenticator, JwtHttpHandler jwtHttpHandler) {
    this.settings = settings;
    this.userIdentityAuthenticator = userIdentityAuthenticator;
    this.jwtHttpHandler = jwtHttpHandler;
  }

  public Optional<UserDto> authenticate(HttpServletRequest request, HttpServletResponse response) {
    if (!settings.getBoolean(ENABLE_PARAM)) {
      return Optional.empty();
    }
    String login = getHeaderValue(request, LOGIN_HEADER_PARAM);
    if (login == null) {
      return Optional.empty();
    }
    UserDto userDto = doAuthenticate(request, login);

    Optional<UserDto> userFromToken = jwtHttpHandler.validateToken(request, response);
    if (userFromToken.isPresent() && userDto.getLogin().equals(userFromToken.get().getLogin())) {
      // User is already authenticated
      return userFromToken;
    }
    jwtHttpHandler.generateToken(userDto, request, response);
    return Optional.of(userDto);
  }

  private UserDto doAuthenticate(HttpServletRequest request, String login) {
    String name = getHeaderValue(request, NAME_HEADER_PARAM);
    String email = getHeaderValue(request, EMAIL_HEADER_PARAM);
    UserIdentity.Builder userIdentityBuilder = UserIdentity.builder()
      .setLogin(login)
      .setName(name == null ? login : name)
      .setEmail(email)
      .setProviderLogin(login);
    if (hasHeader(request, GROUPS_HEADER_PARAM)) {
      String groupsValue = getHeaderValue(request, GROUPS_HEADER_PARAM);
      userIdentityBuilder.setGroups(groupsValue == null ? Collections.emptySet() : new HashSet<>(COMA_SPLITTER.splitToList(groupsValue)));
    }
    return userIdentityAuthenticator.authenticate(userIdentityBuilder.build(), new SsoIdentityProvider());
  }

  @CheckForNull
  private String getHeaderValue(HttpServletRequest request, String settingKey) {
    String headerName = getSettingValue(settingKey);
    if (!isEmpty(headerName)) {
      return request.getHeader(headerName);
    }
    return null;
  }

  private boolean hasHeader(HttpServletRequest request, String settingKey) {
    String headerName = getSettingValue(settingKey);
    return Collections.list(request.getHeaderNames()).stream().anyMatch(header -> header.equals(headerName));
  }

  private String getSettingValue(String settingKey) {
    return defaultIfBlank(settings.getString(settingKey), DEFAULT_VALUES_BY_PARAMETERS.get(settingKey));
  }

  private static class SsoIdentityProvider implements IdentityProvider {
    @Override
    public String getKey() {
      return SQ_AUTHORITY;
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
