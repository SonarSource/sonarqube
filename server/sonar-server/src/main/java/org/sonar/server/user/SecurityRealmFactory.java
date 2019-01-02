/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.security.LoginPasswordAuthenticator;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static org.sonar.process.ProcessProperties.Property.SONAR_AUTHENTICATOR_IGNORE_STARTUP_FAILURE;
import static org.sonar.process.ProcessProperties.Property.SONAR_SECURITY_REALM;

/**
 * @since 2.14
 */
@ServerSide
public class SecurityRealmFactory implements Startable {

  private final boolean ignoreStartupFailure;
  private final SecurityRealm realm;

  public SecurityRealmFactory(Configuration config, SecurityRealm[] realms, LoginPasswordAuthenticator[] authenticators) {
    ignoreStartupFailure = config.getBoolean(SONAR_AUTHENTICATOR_IGNORE_STARTUP_FAILURE.getKey()).orElse(false);
    String realmName = config.get(SONAR_SECURITY_REALM.getKey()).orElse(null);
    String className = config.get(CoreProperties.CORE_AUTHENTICATOR_CLASS).orElse(null);
    SecurityRealm selectedRealm = null;
    if (!StringUtils.isEmpty(realmName)) {
      selectedRealm = selectRealm(realms, realmName);
      if (selectedRealm == null) {
        throw new SonarException(String.format(
          "Realm '%s' not found. Please check the property '%s' in conf/sonar.properties", realmName, SONAR_SECURITY_REALM.getKey()));
      }
    }
    if (selectedRealm == null && !StringUtils.isEmpty(className)) {
      LoginPasswordAuthenticator authenticator = selectAuthenticator(authenticators, className);
      if (authenticator == null) {
        throw new SonarException(String.format(
          "Authenticator '%s' not found. Please check the property '%s' in conf/sonar.properties", className, CoreProperties.CORE_AUTHENTICATOR_CLASS));
      }
      selectedRealm = new CompatibilityRealm(authenticator);
    }
    realm = selectedRealm;
  }

  public SecurityRealmFactory(Configuration config, LoginPasswordAuthenticator[] authenticators) {
    this(config, new SecurityRealm[0], authenticators);
  }

  public SecurityRealmFactory(Configuration config, SecurityRealm[] realms) {
    this(config, realms, new LoginPasswordAuthenticator[0]);
  }

  public SecurityRealmFactory(Configuration config) {
    this(config, new SecurityRealm[0], new LoginPasswordAuthenticator[0]);
  }

  @Override
  public void start() {
    if (realm != null) {
      Logger logger = Loggers.get("org.sonar.INFO");
      try {
        logger.info("Security realm: " + realm.getName());
        realm.init();
        logger.info("Security realm started");
      } catch (RuntimeException e) {
        if (ignoreStartupFailure) {
          logger.error("IGNORED - Security realm fails to start: " + e.getMessage());
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
      if (StringUtils.equals(realmName, realm.getName())) {
        return realm;
      }
    }
    return null;
  }

  private static LoginPasswordAuthenticator selectAuthenticator(LoginPasswordAuthenticator[] authenticators, String className) {
    for (LoginPasswordAuthenticator lpa : authenticators) {
      if (lpa.getClass().getName().equals(className)) {
        return lpa;
      }
    }
    return null;
  }

}
