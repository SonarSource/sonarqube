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
package org.sonar.server.platform.monitoring.cluster;

import com.google.common.base.Joiner;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.process.systeminfo.Global;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.authentication.IdentityProviderRepository;
import org.sonar.server.user.SecurityRealmFactory;

import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

@ServerSide
public class GlobalSystemSection implements SystemInfoSection, Global {
  private static final Joiner COMMA_JOINER = Joiner.on(", ");

  private final Configuration config;
  private final Server server;
  private final SecurityRealmFactory securityRealmFactory;
  private final IdentityProviderRepository identityProviderRepository;

  public GlobalSystemSection(Configuration config, Server server, SecurityRealmFactory securityRealmFactory,
    IdentityProviderRepository identityProviderRepository) {
    this.config = config;
    this.server = server;
    this.securityRealmFactory = securityRealmFactory;
    this.identityProviderRepository = identityProviderRepository;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("System");

    setAttribute(protobuf, "Server ID", server.getId());
    setAttribute(protobuf, "High Availability", true);
    setAttribute(protobuf, "External User Authentication", getExternalUserAuthentication());
    addIfNotEmpty(protobuf, "Accepted external identity providers", getEnabledIdentityProviders());
    addIfNotEmpty(protobuf, "External identity providers whose users are allowed to sign themselves up", getAllowsToSignUpEnabledIdentityProviders());
    setAttribute(protobuf, "Force authentication", getForceAuthentication());
    return protobuf.build();
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

  private static void addIfNotEmpty(ProtobufSystemInfo.Section.Builder protobuf, String key, @Nullable List<String> values) {
    if (values != null && !values.isEmpty()) {
      setAttribute(protobuf, key, COMMA_JOINER.join(values));
    }
  }

  @CheckForNull
  private String getExternalUserAuthentication() {
    SecurityRealm realm = securityRealmFactory.getRealm();
    return realm == null ? null : realm.getName();
  }

}
