/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.platform.monitoring;

import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.platform.Platform;
import org.sonar.server.user.SecurityRealmFactory;

import static org.sonar.api.utils.DateUtils.formatDateTime;

public class SonarQubeMonitoring extends MonitoringMBean implements SonarQubeMonitoringMBean {

  private final Settings settings;
  private final SecurityRealmFactory securityRealmFactory;

  public SonarQubeMonitoring(Settings settings, SecurityRealmFactory securityRealmFactory) {
    this.settings = settings;
    this.securityRealmFactory = securityRealmFactory;
  }

  @Override
  public String getServerId() {
    return sonarProperty(CoreProperties.PERMANENT_SERVER_ID);
  }

  @Override
  public String getVersion() {
    return Platform.getServer().getVersion();
  }

  @Override
  public String getStartedAt() {
    return formatDateTime(Platform.getServer().getStartedAt());
  }

  @Override
  public String getExternalUserAuthentication() {
    if (securityRealmFactory.getRealm() == null) {
      return "";
    }

    return securityRealmFactory.getRealm().getName();
  }

  @Override
  public String getAutomaticUserCreation() {
    return sonarProperty(CoreProperties.CORE_AUTHENTICATOR_CREATE_USERS);
  }

  @Override
  public String getAllowUsersToSignUp() {
    return sonarProperty(CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_PROPERTY);
  }

  @Override
  public String getForceAuthentication() {
    return sonarProperty(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY);
  }

  @Override
  public String name() {
    return "SonarQube";
  }

  @Override
  public void toJson(JsonWriter json) {
    json.beginObject()
      .prop("Server ID", getServerId())
      .prop("Version", getVersion())
      .prop("Started at", getStartedAt())
      .prop("External User Authentication", getExternalUserAuthentication())
      .prop("Automatic User Creation", getAutomaticUserCreation())
      .prop("Allow Users to Sign Up", getAllowUsersToSignUp())
      .prop("Force authentication", getForceAuthentication())
      .endObject();
  }

  private String sonarProperty(String key) {
    return settings.getString(key);
  }
}
