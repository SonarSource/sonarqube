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
package org.sonar.server.user;

import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.SonarException;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.commons.lang3.Strings.CS;
import static org.sonar.process.ProcessProperties.Property.SONAR_AUTHENTICATOR_IGNORE_STARTUP_FAILURE;
import static org.sonar.process.ProcessProperties.Property.SONAR_SECURITY_REALM;

/**
 * @since 2.14
 */
@ServerSide
public class SecurityRealmFactory implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger("org.sonar.INFO");
  private static final String LDAP_SECURITY_REALM = "LDAP";
  private final boolean ignoreStartupFailure;
  private final SecurityRealm realm;

  @Autowired(required = false)
  public SecurityRealmFactory(Configuration config, SecurityRealm[] realms) {
    ignoreStartupFailure = config.getBoolean(SONAR_AUTHENTICATOR_IGNORE_STARTUP_FAILURE.getKey()).orElse(false);
    String realmName = config.get(SONAR_SECURITY_REALM.getKey()).orElse(null);

    if (LDAP_SECURITY_REALM.equals(realmName)) {
      realm = null;
      return;
    }

    SecurityRealm selectedRealm = null;
    if (!StringUtils.isEmpty(realmName)) {
      selectedRealm = selectRealm(realms, realmName);
      if (selectedRealm == null) {
        throw new SonarException(String.format(
          "Realm '%s' not found. Please check the property '%s' in conf/sonar.properties", realmName, SONAR_SECURITY_REALM.getKey()));
      }
    }

    realm = selectedRealm;

  }

  @Autowired(required = false)
  public SecurityRealmFactory(Configuration config) {
    this(config, new SecurityRealm[0]);
  }

  @Override
  public void start() {
    if (realm != null) {
      try {
        LOG.info("Security realm: {}", realm.getName());
        realm.init();
        LOG.info("Security realm started");
      } catch (RuntimeException e) {
        if (ignoreStartupFailure) {
          LOG.error("IGNORED - Security realm fails to start: {}", e.getMessage());
        } else {
          throw new SonarException("Security realm fails to start: " + e.getMessage(), e);
        }
      }
    }
  }

  @Override
  public void stop() {
    // nothing
  }

  @Nullable
  public SecurityRealm getRealm() {
    return realm;
  }

  public boolean hasExternalAuthentication() {
    return getRealm() != null;
  }

  private static SecurityRealm selectRealm(SecurityRealm[] realms, String realmName) {
    for (SecurityRealm realm : realms) {
      if (CS.equals(realmName, realm.getName())) {
        return realm;
      }
    }
    return null;
  }
}
