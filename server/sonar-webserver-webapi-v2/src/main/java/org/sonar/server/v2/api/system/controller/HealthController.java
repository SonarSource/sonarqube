/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2.api.system.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Nullable;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.health.Health;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.user.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.net.HttpURLConnection.HTTP_NOT_IMPLEMENTED;
import static org.sonar.server.v2.WebApiEndpoints.HEALTH_ENDPOINT;

/*
This controller does not support the cluster mode.
This is not the final implementation, as we have to first define what are endpoint contracts.
*/
@RestController
@RequestMapping(HEALTH_ENDPOINT)
@Tag(name = "System")
public class HealthController {

  private final HealthChecker healthChecker;
  private final SystemPasscode systemPasscode;
  private final NodeInformation nodeInformation;
  private final UserSession userSession;

  @Autowired(required=true)
  public HealthController(HealthChecker healthChecker, SystemPasscode systemPasscode, @Nullable NodeInformation nodeInformation,
    @Nullable UserSession userSession) {
    this.healthChecker = healthChecker;
    this.systemPasscode = systemPasscode;
    this.nodeInformation = nodeInformation;
    this.userSession = userSession;
  }

  @GetMapping
  public Health getHealth(@RequestHeader(value = "X-Sonar-Passcode", required = false) String requestPassCode) {
    if (systemPasscode.isValidPasscode(requestPassCode) || isSystemAdmin()) {
      return getHealth();
    }
    throw new ForbiddenException("Insufficient privileges");
  }

  private Health getHealth() {
    if (nodeInformation == null || nodeInformation.isStandalone()) {
      return healthChecker.checkNode();
    }
    throw new ServerException(HTTP_NOT_IMPLEMENTED, "Unsupported in cluster mode");
  }

  private boolean isSystemAdmin() {
    if (userSession == null) {
      return false;
    }
    return userSession.isSystemAdministrator();
  }
}
