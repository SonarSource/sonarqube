/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.ServerComponent;
import org.sonar.api.security.LoginPasswordAuthenticator;

public class AuthenticatorFactory implements ServerComponent {

  private static final Logger LOG = LoggerFactory.getLogger(AuthenticatorFactory.class);
  private static final Logger INFO = LoggerFactory.getLogger("org.sonar.INFO");

  private LoginPasswordAuthenticator authenticator = null;
  private String classname;
  private boolean ignoreStartupFailure;
  private LoginPasswordAuthenticator[] authenticators;

  public AuthenticatorFactory(Configuration configuration, LoginPasswordAuthenticator[] authenticators) {
    classname = configuration.getString(CoreProperties.CORE_AUTHENTICATOR_CLASS);
    ignoreStartupFailure = configuration.getBoolean(CoreProperties.CORE_AUTHENTICATOR_IGNORE_STARTUP_FAILURE, false);
    this.authenticators = authenticators;
  }

  /**
   * This constructor is used when there aren't any authentication plugins.
   */
  public AuthenticatorFactory(Configuration configuration) {
    this(configuration, null);
  }

  /**
   * Start the authenticator selected in sonar configuration. If no authentication plugin is selected, then
   * the default authentication mechanism is used and null is returned.
   * <p/>
   * Throws a unchecked exception if the authenticator can not be started.
   */

  public void start() {
    // check authentication plugin at startup
    if (StringUtils.isEmpty(classname)) {
      // use sonar internal authenticator
      return;
    }

    authenticator = searchAuthenticator();
    if (authenticator == null) {
      LOG.error("Authentication plugin not found. Please check the property '" + CoreProperties.CORE_AUTHENTICATOR_CLASS + "' in conf/sonar.properties");
      throw new AuthenticatorNotFoundException(classname);
    }

    try {
      INFO.info("Authentication plugin: class " + classname);
      authenticator.init();
      INFO.info("Authentication plugin started");

    } catch (RuntimeException e) {
      if (ignoreStartupFailure) {
        LOG.error("IGNORED - Authentication plugin fails to start: " + e.getMessage());
      } else {
        LOG.error("Authentication plugin fails to start: " + e.getMessage());
        throw e;
      }
    }
  }

  public LoginPasswordAuthenticator getAuthenticator() {
    return authenticator;
  }

  private LoginPasswordAuthenticator searchAuthenticator() {
    if (authenticators != null) {
      for (LoginPasswordAuthenticator lpa : authenticators) {
        if (lpa.getClass().getName().equals(classname)) {
          return lpa;
        }
      }
    }
    return null;
  }
}
