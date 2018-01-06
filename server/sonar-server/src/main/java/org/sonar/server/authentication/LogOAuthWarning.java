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
package org.sonar.server.authentication;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.Startable;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.utils.log.Loggers;

public class LogOAuthWarning implements Startable {

  private final Server server;
  private final OAuth2IdentityProvider[] providers;

  public LogOAuthWarning(Server server, OAuth2IdentityProvider[] providers) {
    this.server = server;
    this.providers = providers;
  }

  /**
   * Used by default by picocontainer when no OAuth2IdentityProvider are present
   */
  public LogOAuthWarning(Server server) {
    this(server, new OAuth2IdentityProvider[0]);
  }

  @Override
  public void start() {
    if (providers.length == 0) {
      return;
    }
    String publicRootUrl = server.getPublicRootUrl();
    if (StringUtils.startsWithIgnoreCase(publicRootUrl, "http:")) {
      Loggers.get(getClass()).warn(
        "For security reasons, OAuth authentication should use HTTPS. You should set the property 'Administration > Configuration > Server base URL' to a HTTPS URL.");
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
