/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.exceptions.BadRequestException;

import static org.apache.commons.lang.time.DateUtils.addMinutes;
import static org.sonar.server.user.ExternalIdentity.SQ_AUTHORITY;

public class SsoAuthenticator implements Startable {

  private static final Logger LOG = Loggers.get(SsoAuthenticator.class);

  private static final Splitter COMA_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

  private static final String ENABLE_PARAM = "sonar.web.sso.enable";

  private static final String LOGIN_HEADER_PARAM = "sonar.web.sso.loginHeader";
  private static final String LOGIN_HEADER_DEFAULT_VALUE = "X-Forwarded-Login";

  private static final String NAME_HEADER_PARAM = "sonar.web.sso.nameHeader";
  private static final String NAME_HEADER_DEFAULT_VALUE = "X-Forwarded-Name";

  private static final String EMAIL_HEADER_PARAM = "sonar.web.sso.emailHeader";
  private static final String EMAIL_HEADER_DEFAULT_VALUE = "X-Forwarded-Email";

  private static final String GROUPS_HEADER_PARAM = "sonar.web.sso.groupsHeader";
  private static final String GROUPS_HEADER_DEFAULT_VALUE = "X-Forwarded-Groups";

  private static final String REFRESH_INTERVAL_PARAM = "sonar.web.sso.refreshIntervalInMinutes";
  private static final String REFRESH_INTERVAL_DEFAULT_VALUE = "5";

  private static final String LAST_REFRESH_TIME_TOKEN_PARAM = "ssoLastRefreshTime";

  private static final Map<String, String> DEFAULT_VALUES_BY_SETTING_KEYS = ImmutableMap.of(
    LOGIN_HEADER_PARAM, LOGIN_HEADER_DEFAULT_VALUE,
    NAME_HEADER_PARAM, NAME_HEADER_DEFAULT_VALUE,
    EMAIL_HEADER_PARAM, EMAIL_HEADER_DEFAULT_VALUE,
    GROUPS_HEADER_PARAM, GROUPS_HEADER_DEFAULT_VALUE,
    REFRESH_INTERVAL_PARAM, REFRESH_INTERVAL_DEFAULT_VALUE);

  private final System2 system2;
  private final Configuration config;
  private final UserIdentityAuthenticator userIdentityAuthenticator;
  private final JwtHttpHandler jwtHttpHandler;
  private final AuthenticationEvent authenticationEvent;

  private boolean enabled = false;
  private Map<String, String> settingsByKey = new HashMap<>();

  public SsoAuthenticator(System2 system2, Configuration config, UserIdentityAuthenticator userIdentityAuthenticator,
    JwtHttpHandler jwtHttpHandler, AuthenticationEvent authenticationEvent) {
    this.system2 = system2;
    this.config = config;
    this.userIdentityAuthenticator = userIdentityAuthenticator;
    this.jwtHttpHandler = jwtHttpHandler;
    this.authenticationEvent = authenticationEvent;
  }

  @Override
  public void start() {
    if (config.getBoolean(ENABLE_PARAM).orElse(false)) {
      LOG.info("SSO Authentication enabled");
      enabled = true;
      DEFAULT_VALUES_BY_SETTING_KEYS.entrySet()
        .forEach(entry -> settingsByKey.put(entry.getKey(), config.get(entry.getKey()).orElse(DEFAULT_VALUES_BY_SETTING_KEYS.get(entry.getKey()))));
    }
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  public Optional<UserDto> authenticate(HttpServletRequest request, HttpServletResponse response) {
    try {
      return doAuthenticate(request, response);
    } catch (BadRequestException e) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.sso())
        .setMessage(e.getMessage())
        .build();
    }
  }

  private Optional<UserDto> doAuthenticate(HttpServletRequest request, HttpServletResponse response) {
    if (!enabled) {
      return Optional.empty();
    }
    Map<String, String> headerValuesByNames = getHeaders(request);
    String login = getHeaderValue(headerValuesByNames, LOGIN_HEADER_PARAM);
    if (login == null) {
      return Optional.empty();
    }
    Optional<UserDto> user = getUserFromToken(request, response);
    if (user.isPresent() && login.equals(user.get().getLogin())) {
      return user;
    }

    UserDto userDto = doAuthenticate(headerValuesByNames, login);
    jwtHttpHandler.generateToken(userDto, ImmutableMap.of(LAST_REFRESH_TIME_TOKEN_PARAM, system2.now()), request, response);
    authenticationEvent.loginSuccess(request, userDto.getLogin(), Source.sso());
    return Optional.of(userDto);
  }

  private Optional<UserDto> getUserFromToken(HttpServletRequest request, HttpServletResponse response) {
    Optional<JwtHttpHandler.Token> token = jwtHttpHandler.getToken(request, response);
    if (!token.isPresent()) {
      return Optional.empty();
    }
    Date now = new Date(system2.now());
    int refreshIntervalInMinutes = Integer.parseInt(settingsByKey.get(REFRESH_INTERVAL_PARAM));
    Long lastFreshTime = (Long) token.get().getProperties().get(LAST_REFRESH_TIME_TOKEN_PARAM);
    if (lastFreshTime == null || now.after(addMinutes(new Date(lastFreshTime), refreshIntervalInMinutes))) {
      return Optional.empty();
    }
    return Optional.of(token.get().getUserDto());
  }

  private UserDto doAuthenticate(Map<String, String> headerValuesByNames, String login) {
    String name = getHeaderValue(headerValuesByNames, NAME_HEADER_PARAM);
    String email = getHeaderValue(headerValuesByNames, EMAIL_HEADER_PARAM);
    UserIdentity.Builder userIdentityBuilder = UserIdentity.builder()
      .setLogin(login)
      .setName(name == null ? login : name)
      .setEmail(email)
      .setProviderLogin(login);
    if (hasHeader(headerValuesByNames, GROUPS_HEADER_PARAM)) {
      String groupsValue = getHeaderValue(headerValuesByNames, GROUPS_HEADER_PARAM);
      userIdentityBuilder.setGroups(groupsValue == null ? Collections.emptySet() : new HashSet<>(COMA_SPLITTER.splitToList(groupsValue)));
    }
    return userIdentityAuthenticator.authenticate(userIdentityBuilder.build(), new SsoIdentityProvider(), Source.sso());
  }

  @CheckForNull
  private String getHeaderValue(Map<String, String> headerValuesByNames, String settingKey) {
    return headerValuesByNames.get(settingsByKey.get(settingKey).toLowerCase(Locale.ENGLISH));
  }

  private static Map<String, String> getHeaders(HttpServletRequest request) {
    Map<String, String> headers = new HashMap<>();
    Collections.list(request.getHeaderNames()).forEach(header -> headers.put(header.toLowerCase(Locale.ENGLISH), request.getHeader(header)));
    return headers;
  }

  private boolean hasHeader(Map<String, String> headerValuesByNames, String settingKey) {
    return headerValuesByNames.keySet().contains(settingsByKey.get(settingKey).toLowerCase(Locale.ENGLISH));
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
