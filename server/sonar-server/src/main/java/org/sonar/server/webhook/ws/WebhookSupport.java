/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.webhook.ws;

import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;

public class WebhookSupport {

  private final UserSession userSession;

  public WebhookSupport(UserSession userSession) {
    this.userSession = userSession;
  }

  void checkUserPermissionOn(ComponentDto componentDto) {
    userSession.checkComponentPermission(ADMIN, componentDto);
  }

  void checkUserPermissionOn(OrganizationDto organizationDto) {
    userSession.checkPermission(ADMINISTER, organizationDto);
  }

  void checkUrlPattern(String url, String message, Object... messageArguments) {
    if (!url.toLowerCase(ENGLISH).startsWith("http://") && !url.toLowerCase(ENGLISH).startsWith("https://")) {
      throw new IllegalArgumentException(format(message, messageArguments));
    }
    String sub = url.substring("http://".length());
    if (sub.contains(":") && !sub.substring(sub.indexOf(':')).contains("@")) {
      throw new IllegalArgumentException(format(message, messageArguments));
    }
  }

}
