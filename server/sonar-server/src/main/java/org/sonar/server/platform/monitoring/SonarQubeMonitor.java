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
package org.sonar.server.platform.monitoring;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.core.util.stream.Collectors;
import org.sonar.process.ProcessProperties;
import org.sonar.server.authentication.IdentityProviderRepository;
import org.sonar.server.platform.ServerId;
import org.sonar.server.platform.ServerIdLoader;
import org.sonar.server.platform.ServerLogging;
import org.sonar.server.user.SecurityRealmFactory;

public class SonarQubeMonitor extends BaseMonitorMBean implements SonarQubeMonitorMBean {

  private static final Joiner COMMA_JOINER = Joiner.on(", ");

  static final String BRANDING_FILE_PATH = "web/WEB-INF/classes/com/sonarsource/branding";

  private final Settings settings;
  private final SecurityRealmFactory securityRealmFactory;
  private final IdentityProviderRepository identityProviderRepository;
  private final Server server;
  private final ServerLogging serverLogging;
  private final ServerIdLoader serverIdLoader;

  public SonarQubeMonitor(Settings settings, SecurityRealmFactory securityRealmFactory,
    IdentityProviderRepository identityProviderRepository, Server server, ServerLogging serverLogging,
    ServerIdLoader serverIdLoader) {
    this.settings = settings;
    this.securityRealmFactory = securityRealmFactory;
    this.identityProviderRepository = identityProviderRepository;
    this.server = server;
    this.serverLogging = serverLogging;
    this.serverIdLoader = serverIdLoader;
  }

  @Override
  public String getServerId() {
    return serverIdLoader.getRaw().orNull();
  }

  @Override
  public String getVersion() {
    return server.getVersion();
  }

  @Override
  public String getLogLevel() {
    return serverLogging.getRootLoggerLevel().name();
  }

  @CheckForNull
  private String getExternalUserAuthentication() {
    SecurityRealm realm = securityRealmFactory.getRealm();
    return realm == null ? null : realm.getName();
  }

  private List<String> getEnabledIdentityProviders() {
    return identityProviderRepository.getAllEnabledAndSorted()
      .stream()
      .filter(IdentityProvider::isEnabled)
      .map(IdentityProvider::getName)
      .collect(Collectors.toList());
  }

  private List<String> getAllowsToSignUpEnabledIdentityProviders() {
    return identityProviderRepository.getAllEnabledAndSorted()
      .stream()
      .filter(IdentityProvider::isEnabled)
      .filter(IdentityProvider::allowsUsersToSignUp)
      .map(IdentityProvider::getName)
      .collect(Collectors.toList());
  }

  private boolean getAutomaticUserCreation() {
    return settings.getBoolean(CoreProperties.CORE_AUTHENTICATOR_CREATE_USERS);
  }

  private boolean getAllowUsersToSignUp() {
    return settings.getBoolean(CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_PROPERTY);
  }

  private boolean getForceAuthentication() {
    return settings.getBoolean(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY);
  }

  private boolean isOfficialDistribution() {
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
  public Map<String, Object> attributes() {
    Map<String, Object> attributes = new LinkedHashMap<>();
    completeWithServerIdAttributes(attributes);
    attributes.put("Version", getVersion());
    addIfNotNull("External User Authentication", getExternalUserAuthentication(), attributes);
    addIfNotEmpty("Accepted external identity providers", getEnabledIdentityProviders(), attributes);
    addIfNotEmpty("External identity providers whose users are allowed to sign themselves up", getAllowsToSignUpEnabledIdentityProviders(), attributes);
    attributes.put("Automatic User Creation", getAutomaticUserCreation());
    attributes.put("Allow Users to Sign Up", getAllowUsersToSignUp());
    attributes.put("Force authentication", getForceAuthentication());
    attributes.put("Official Distribution", isOfficialDistribution());
    attributes.put("Home Dir", settings.getString(ProcessProperties.PATH_HOME));
    attributes.put("Data Dir", settings.getString(ProcessProperties.PATH_DATA));
    attributes.put("Temp Dir", settings.getString(ProcessProperties.PATH_TEMP));
    attributes.put("Logs Dir", settings.getString(ProcessProperties.PATH_LOGS));
    attributes.put("Logs Level", getLogLevel());
    return attributes;
  }

  private void completeWithServerIdAttributes(Map<String, Object> attributes) {
    Optional<ServerId> serverId = serverIdLoader.get();
    if (serverId.isPresent()) {
      attributes.put("Server ID", serverId.get().getId());
      attributes.put("Server ID validated", serverId.get().isValid());
    }
  }

  private static void addIfNotNull(String key, @Nullable String value, Map<String, Object> attributes) {
    if (value != null) {
      attributes.put(key, value);
    }
  }

  private static void addIfNotEmpty(String key, List<String> values, Map<String, Object> attributes) {
    if (!values.isEmpty()) {
      attributes.put(key, COMMA_JOINER.join(values));
    }
  }
}
