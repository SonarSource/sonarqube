/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.user;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.Request;

import static org.sonar.process.ProcessProperties.Property.WEB_SYSTEM_PASS_CODE;

public class BearerPasscode {

  public static final String PASSCODE_HTTP_HEADER = "Authorization";

  private final Configuration configuration;

  public BearerPasscode(Configuration configuration) {
    this.configuration = configuration;
  }

  public boolean isValid(Request request) {
    Optional<String> passcodeOpt = configuration.get(WEB_SYSTEM_PASS_CODE.getKey()).map(StringUtils::trimToNull);

    if (passcodeOpt.isEmpty()) {
      return false;
    }

    String configuredPasscode = passcodeOpt.get();
    return request.header(PASSCODE_HTTP_HEADER)
      .map(s -> s.replace("Bearer ", ""))
      .map(configuredPasscode::equals)
      .orElse(false);
  }

}
