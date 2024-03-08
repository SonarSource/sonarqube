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

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.Request;
import org.slf4j.LoggerFactory;

import static org.sonar.process.ProcessProperties.Property.WEB_SYSTEM_PASS_CODE;

@ServerSide
public class SystemPasscodeImpl implements SystemPasscode, Startable {

  public static final String PASSCODE_HTTP_HEADER = "X-Sonar-Passcode";

  private final Configuration configuration;
  private String configuredPasscode;

  public SystemPasscodeImpl(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public boolean isValid(Request request) {
    if (configuredPasscode == null) {
      return false;
    }
    return isValidPasscode(request.header(PASSCODE_HTTP_HEADER).orElse(null));
  }

  @Override
  public boolean isValidPasscode(@Nullable String passcode) {
    return Optional.ofNullable(passcode)
      .map(s -> Objects.equals(configuredPasscode, s))
      .orElse(false);
  }

  @Override
  public void start() {
    Optional<String> passcodeOpt = configuration.get(WEB_SYSTEM_PASS_CODE.getKey())
      // if present, result is never empty string
      .map(StringUtils::trimToNull);

    if (passcodeOpt.isPresent()) {
      logState("enabled");
      configuredPasscode = passcodeOpt.get();
    } else {
      logState("disabled");
      configuredPasscode = null;
    }
  }

  private void logState(String state) {
    LoggerFactory.getLogger(getClass()).info("System authentication by passcode is {}", state);
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
