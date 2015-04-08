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
import org.sonar.api.platform.Server;
import org.sonar.api.security.SecurityRealm;
import org.sonar.process.ProcessProperties;
import org.sonar.server.user.SecurityRealmFactory;

import java.io.File;
import java.util.LinkedHashMap;

public class SonarQubeMonitor extends BaseMonitorMBean implements SonarQubeMonitorMBean {

  static final String BRANDING_FILE_PATH = "web/WEB-INF/classes/com/sonarsource/branding";

  private final Settings settings;
  private final SecurityRealmFactory securityRealmFactory;
  private final Server server;

  public SonarQubeMonitor(Settings settings, SecurityRealmFactory securityRealmFactory,
    Server server) {
    this.settings = settings;
    this.securityRealmFactory = securityRealmFactory;
    this.server = server;
  }

  @Override
  public String getServerId() {
    return settings.getString(CoreProperties.PERMANENT_SERVER_ID);
  }

  @Override
  public String getVersion() {
    return server.getVersion();
  }

  public String getExternalUserAuthentication() {
    SecurityRealm realm = securityRealmFactory.getRealm();
    if (realm == null) {
      return "";
    }
    return realm.getName();
  }

  public boolean getAutomaticUserCreation() {
    return settings.getBoolean(CoreProperties.CORE_AUTHENTICATOR_CREATE_USERS);
  }

  public boolean getAllowUsersToSignUp() {
    return settings.getBoolean(CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_PROPERTY);
  }

  public boolean getForceAuthentication() {
    return settings.getBoolean(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY);
  }

  public boolean isOfficialDistribution() {
    // the dependency com.sonarsource:sonarsource-branding is shaded to webapp
    // during release (see sonar-web pom)
    File brandingFile = new File(server.getRootDir(), BRANDING_FILE_PATH);
    // no need to check that the file exists. java.io.File#length() returns zero in this case.
    return brandingFile.length() > 0L;
  }

  @Override
  public String name() {
    return "SonarQube";
  }

  @Override
  public LinkedHashMap<String, Object> attributes() {
    LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("Server ID", getServerId());
    attributes.put("Version", getVersion());
    attributes.put("External User Authentication", getExternalUserAuthentication());
    attributes.put("Automatic User Creation", getAutomaticUserCreation());
    attributes.put("Allow Users to Sign Up", getAllowUsersToSignUp());
    attributes.put("Force authentication", getForceAuthentication());
    attributes.put("Official Distribution", isOfficialDistribution());
    attributes.put("Home Dir", settings.getString(ProcessProperties.PATH_HOME));
    attributes.put("Data Dir", settings.getString(ProcessProperties.PATH_DATA));
    attributes.put("Logs Dir", settings.getString(ProcessProperties.PATH_LOGS));
    attributes.put("Temp Dir", settings.getString(ProcessProperties.PATH_TEMP));
    return attributes;

  }
}
