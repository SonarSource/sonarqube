/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.auth.ldap;

import javax.annotation.Nullable;

import static org.sonar.api.utils.Preconditions.checkState;

public final class LdapAuthenticationResult {

  private final boolean success;

  private final String serverKey;

  private LdapAuthenticationResult(boolean success, @Nullable String serverKey) {
    this.success = success;
    this.serverKey = serverKey;
  }

  public static LdapAuthenticationResult failed() {
    return new LdapAuthenticationResult(false, null);
  }

  public static LdapAuthenticationResult success(String serverKey) {
    return new LdapAuthenticationResult(true, serverKey);
  }

  public boolean isSuccess() {
    return success;
  }

  public String getServerKey() {
    checkState(isSuccess(), "serverKey is only set for successful authentication.");
    return serverKey;
  }
}
