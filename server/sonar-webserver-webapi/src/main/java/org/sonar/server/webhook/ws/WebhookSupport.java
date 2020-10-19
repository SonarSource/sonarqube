/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import okhttp3.HttpUrl;
import org.sonar.api.config.Configuration;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.process.ProcessProperties.Property.SONAR_VALIDATE_WEBHOOKS;

public class WebhookSupport {

  private final UserSession userSession;
  private final Configuration configuration;

  public WebhookSupport(UserSession userSession, Configuration configuration) {
    this.userSession = userSession;
    this.configuration = configuration;
  }

  void checkPermission(ProjectDto projectDto) {
    userSession.checkProjectPermission(ADMIN, projectDto);
  }

  void checkPermission() {
    userSession.checkPermission(ADMINISTER);
  }

  void checkUrlPattern(String url, String message, Object... messageArguments) {
    try {
      HttpUrl okUrl = HttpUrl.parse(url);
      if (okUrl == null) {
        throw new IllegalArgumentException(String.format(message, messageArguments));
      }
      InetAddress address = InetAddress.getByName(okUrl.host());
      if (configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS.getKey()).orElse(true)
        && (address.isLoopbackAddress() || address.isAnyLocalAddress())) {
        throw new IllegalArgumentException("Invalid URL: loopback and wildcard addresses are not allowed for webhooks.");
      }
    } catch (UnknownHostException e) {
      // if a host can not be resolved the deliveries will fail - no need to block it from being set
      // this will only happen for public URLs
    }
  }

  void checkThatProjectBelongsToOrganization(ProjectDto projectDto, OrganizationDto organizationDto, String message, Object... messageArguments) {
    if (!organizationDto.getUuid().equals(projectDto.getOrganizationUuid())) {
      throw new NotFoundException(format(message, messageArguments));
    }
  }

}
