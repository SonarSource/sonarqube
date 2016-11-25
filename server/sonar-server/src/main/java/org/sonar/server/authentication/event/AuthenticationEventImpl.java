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
package org.sonar.server.authentication.event;

import com.google.common.base.Joiner;
import java.util.Collections;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.Collectors;

public class AuthenticationEventImpl implements AuthenticationEvent {
  private static final Logger LOGGER = Loggers.get("auth.event");

  @Override
  public void login(HttpServletRequest request, @Nullable String login, Source source) {
    LOGGER.info("login success [method|{}][provider|{}|{}][IP|{}|{}][login|{}]",
        source.getMethod(), source.getProvider(), source.getProviderName(), request.getRemoteAddr(), getAllIps(request),
        login == null ? "" : login);
  }

  private static String getAllIps(HttpServletRequest request) {
    return Collections.list(request.getHeaders("X-Forwarded-For")).stream().collect(Collectors.join(Joiner.on(",")));
  }

}
