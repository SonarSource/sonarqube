/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.ui;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.security.LoginPasswordAuthenticator;
import org.sonar.api.security.Realm;

/**
 * @since 2.14
 */
public class RealmFactory implements ServerComponent {

  private static final Logger INFO = LoggerFactory.getLogger("org.sonar.INFO");
  private static final Logger LOG = LoggerFactory.getLogger(RealmFactory.class);

  private final boolean ignoreStartupFailure;
  private final Realm realm;

  static final String REALM_PROPERTY = "sonar.security.realm";

  public RealmFactory(Settings settings, Realm[] realms, LoginPasswordAuthenticator[] authenticators) {
    ignoreStartupFailure = settings.getBoolean(CoreProperties.CORE_AUTHENTICATOR_IGNORE_STARTUP_FAILURE);
    String realmName = settings.getString(REALM_PROPERTY);
    String className = settings.getString(CoreProperties.CORE_AUTHENTICATOR_CLASS);
    Realm selectedRealm = null;
    if (!StringUtils.isEmpty(realmName)) {
      selectedRealm = selectRealm(realms, realmName);
      if (selectedRealm == null) {
        LOG.error("Realm not found. Please check the property '" + REALM_PROPERTY + "' in conf/sonar.properties");
        throw new AuthenticatorNotFoundException(realmName);
      }
    }
    if (selectedRealm == null && !StringUtils.isEmpty(className)) {
      LoginPasswordAuthenticator authenticator = selectAuthenticator(authenticators, className);
      if (authenticator == null) {
        LOG.error("Authenticator plugin not found. Please check the property '" + CoreProperties.CORE_AUTHENTICATOR_CLASS
          + "' in conf/sonar.properties");
        throw new AuthenticatorNotFoundException(className);
      }
      selectedRealm = new CompatibilityRealm(authenticator);
    }
    realm = selectedRealm;
  }

  public RealmFactory(Settings settings, LoginPasswordAuthenticator[] authenticators) {
    this(settings, null, authenticators);
  }

  public RealmFactory(Settings settings, Realm[] realms) {
    this(settings, realms, null);
  }

  public RealmFactory(Settings settings) {
    this(settings, null, null);
  }

  public void start() {
    if (realm != null) {
      try {
        INFO.info("Security realm: " + realm.getName());
        realm.init();
        INFO.info("Security realm started");
      } catch (RuntimeException e) {
        if (ignoreStartupFailure) {
          LOG.error("IGNORED - Realm fails to start: " + e.getMessage());
        } else {
          LOG.error("Realm fails to start: " + e.getMessage());
          throw e;
        }
      }
    }
  }

  public Realm getRealm() {
    return realm;
  }

  private static Realm selectRealm(Realm[] realms, String realmName) {
    if (realms != null) {
      for (Realm realm : realms) {
        if (StringUtils.equals(realmName, realm.getName())) {
          return realm;
        }
      }
    }
    return null;
  }

  private static LoginPasswordAuthenticator selectAuthenticator(LoginPasswordAuthenticator[] authenticators, String className) {
    if (authenticators != null) {
      for (LoginPasswordAuthenticator lpa : authenticators) {
        if (lpa.getClass().getName().equals(className)) {
          return lpa;
        }
      }
    }
    return null;
  }

}
