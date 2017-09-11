/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.platform.monitoring;

import com.google.common.base.Joiner;
import java.io.File;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.process.ProcessProperties;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.authentication.IdentityProviderRepository;
import org.sonar.server.platform.ServerIdLoader;
import org.sonar.server.platform.ServerLogging;
import org.sonar.server.user.SecurityRealmFactory;

import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

public class SonarQubeSection extends BaseSectionMBean implements SonarQubeSectionMBean {

  private static final Joiner COMMA_JOINER = Joiner.on(", ");

  static final String BRANDING_FILE_PATH = "web/WEB-INF/classes/com/sonarsource/branding";

  private final Configuration config;
  private final SecurityRealmFactory securityRealmFactory;
  private final IdentityProviderRepository identityProviderRepository;
  private final Server server;
  private final ServerLogging serverLogging;
  private final ServerIdLoader serverIdLoader;

  public SonarQubeSection(Configuration config, SecurityRealmFactory securityRealmFactory,
    IdentityProviderRepository identityProviderRepository, Server server, ServerLogging serverLogging,
    ServerIdLoader serverIdLoader) {
    this.config = config;
    this.securityRealmFactory = securityRealmFactory;
    this.identityProviderRepository = identityProviderRepository;
    this.server = server;
    this.serverLogging = serverLogging;
    this.serverIdLoader = serverIdLoader;
  }

  @Override
  public String getServerId() {
    return serverIdLoader.getRaw().orElse(null);
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
      .collect(MoreCollectors.toList());
  }

  private List<String> getAllowsToSignUpEnabledIdentityProviders() {
    return identityProviderRepository.getAllEnabledAndSorted()
      .stream()
      .filter(IdentityProvider::isEnabled)
      .filter(IdentityProvider::allowsUsersToSignUp)
      .map(IdentityProvider::getName)
      .collect(MoreCollectors.toList());
  }

  private boolean getForceAuthentication() {
    return config.getBoolean(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY).orElse(false);
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
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName(name());

    serverIdLoader.get().ifPresent(serverId -> {
      setAttribute(protobuf, "Server ID", serverId.getId());
      setAttribute(protobuf, "Server ID validated", serverId.isValid());
    });
    setAttribute(protobuf, "Version", getVersion());
    setAttribute(protobuf, "External User Authentication", getExternalUserAuthentication());
    addIfNotEmpty(protobuf, "Accepted external identity providers", getEnabledIdentityProviders());
    addIfNotEmpty(protobuf, "External identity providers whose users are allowed to sign themselves up", getAllowsToSignUpEnabledIdentityProviders());
    setAttribute(protobuf, "Force authentication", getForceAuthentication());
    setAttribute(protobuf, "Official Distribution", isOfficialDistribution());
    setAttribute(protobuf, "Home Dir", config.get(ProcessProperties.PATH_HOME).orElse(null));
    setAttribute(protobuf, "Data Dir", config.get(ProcessProperties.PATH_DATA).orElse(null));
    setAttribute(protobuf, "Temp Dir", config.get(ProcessProperties.PATH_TEMP).orElse(null));
    setAttribute(protobuf, "Logs Dir", config.get(ProcessProperties.PATH_LOGS).orElse(null));
    setAttribute(protobuf, "Logs Level", getLogLevel());
    return protobuf.build();
  }

  private static void addIfNotEmpty(ProtobufSystemInfo.Section.Builder protobuf, String key, @Nullable List<String> values) {
    if (values != null && !values.isEmpty()) {
      setAttribute(protobuf, key, COMMA_JOINER.join(values));
    }
  }
}
