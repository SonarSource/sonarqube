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

import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.IdentityProvider;

public class TestIdentityProvider implements IdentityProvider {

  private String key;
  private String name;
  private Display display;
  private boolean enabled;
  private boolean allowsUsersToSignUp;

  @Override
  public String getKey() {
    return key;
  }

  public TestIdentityProvider setKey(String key) {
    this.key = key;
    return this;
  }

  @Override
  public String getName() {
    return name;
  }

  public TestIdentityProvider setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public Display getDisplay() {
    return display;
  }

  public TestIdentityProvider setDisplay(Display display) {
    this.display = display;
    return this;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public TestIdentityProvider setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  @Override
  public boolean allowsUsersToSignUp() {
    return allowsUsersToSignUp;
  }

  public TestIdentityProvider setAllowsUsersToSignUp(boolean allowsUsersToSignUp) {
    this.allowsUsersToSignUp = allowsUsersToSignUp;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (getClass() != o.getClass()) {
      return false;
    }

    TestIdentityProvider that = (TestIdentityProvider) o;
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }
}
