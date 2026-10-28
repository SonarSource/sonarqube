/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonarqube.ws.tester;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

/**
 * Passwords of the users created by the testers. The password policy rejects a password equal to the login,
 * so {@link #forLogin(String)} gives a compliant default, and {@link #of(String)} returns the password to use
 * when logging in as a login (which is the login itself for users that were not created by a tester, like admin).
 */
public final class TestPasswords {

  private static final String COMPLIANT_SUFFIX = "_Str0ngPassw0rd!";
  private static final Map<String, String> PASSWORDS_BY_LOGIN = new ConcurrentHashMap<>();

  private TestPasswords() {
  }

  public static String forLogin(String login) {
    return login + COMPLIANT_SUFFIX;
  }

  static void register(@Nullable String login, @Nullable String password) {
    if (login != null && password != null) {
      PASSWORDS_BY_LOGIN.put(login, password);
    }
  }

  public static String of(String login) {
    return PASSWORDS_BY_LOGIN.getOrDefault(login, login);
  }
}
