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
package org.sonar.server.authentication;

import org.sonar.api.server.authentication.OAuth2IdentityProvider;

class FakeOAuth2IdentityProvider extends TestIdentityProvider implements OAuth2IdentityProvider {

  private boolean initCalled = false;
  private boolean callbackCalled = false;

  public FakeOAuth2IdentityProvider(String key, boolean enabled) {
    setKey(key);
    setName("name of " + key);
    setEnabled(enabled);
  }

  @Override
  public void init(InitContext context) {
    initCalled = true;
  }

  @Override
  public void callback(CallbackContext context) {
    callbackCalled = true;
  }

  public boolean isInitCalled() {
    return initCalled;
  }

  public boolean isCallbackCalled() {
    return callbackCalled;
  }
}
