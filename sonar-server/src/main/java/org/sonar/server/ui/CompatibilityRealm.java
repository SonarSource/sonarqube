/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.sonar.api.CoreProperties;
import org.sonar.api.security.LoginPasswordAuthenticator;
import org.sonar.api.security.Realm;

/**
 * Provides backward compatibility for {@link CoreProperties#CORE_AUTHENTICATOR_CLASS}.
 *
 * @since 2.14
 */
class CompatibilityRealm extends Realm {
  private final LoginPasswordAuthenticator authenticator;

  public CompatibilityRealm(LoginPasswordAuthenticator authenticator) {
    this.authenticator = authenticator;
  }

  @Override
  public void init() {
    authenticator.init();
  }

  @Override
  public String getName() {
    return "CompatibilityRealm[" + authenticator.getClass().getName() + "]";
  }

  @Override
  public LoginPasswordAuthenticator getAuthenticator() {
    return authenticator;
  }
}
