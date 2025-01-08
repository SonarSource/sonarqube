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
package org.sonar.server.v2.api.system.controller;

import javax.annotation.Nullable;
import org.sonar.server.common.platform.LivenessChecker;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.user.UserSession;

public class DefaultLivenessController implements LivenessController {

  private final LivenessChecker livenessChecker;
  private final UserSession userSession;
  private final SystemPasscode systemPasscode;

  public DefaultLivenessController(LivenessChecker livenessChecker, SystemPasscode systemPasscode, @Nullable UserSession userSession) {
    this.livenessChecker = livenessChecker;
    this.userSession = userSession;
    this.systemPasscode = systemPasscode;
  }

  @Override
  public void livenessCheck(String requestPassCode) {
    if (systemPasscode.isValidPasscode(requestPassCode) || isSystemAdmin()) {
      if (livenessChecker.liveness()) {
        return;
      }
      throw new IllegalStateException("Liveness check failed");
    }
    throw new ForbiddenException("Insufficient privileges");
  }

  private boolean isSystemAdmin() {
    if (userSession == null) {
      return false;
    }
    return userSession.isSystemAdministrator();
  }

}
