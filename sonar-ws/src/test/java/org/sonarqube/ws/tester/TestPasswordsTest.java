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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPasswordsTest {

  @Test
  public void forLogin_returns_a_password_different_from_the_login() {
    assertThat(TestPasswords.forLogin("login1"))
      .isNotEqualTo("login1")
      .startsWith("login1")
      .hasSizeGreaterThanOrEqualTo(12)
      .matches(".*[A-Z].*")
      .matches(".*[a-z].*")
      .matches(".*\\d.*")
      .matches(".*[^a-zA-Z0-9].*");
  }

  @Test
  public void of_returns_the_login_when_no_password_is_registered() {
    assertThat(TestPasswords.of("admin-never-registered")).isEqualTo("admin-never-registered");
  }

  @Test
  public void of_returns_the_registered_password() {
    TestPasswords.register("registered-login", "A-registered-Passw0rd");

    assertThat(TestPasswords.of("registered-login")).isEqualTo("A-registered-Passw0rd");
  }

  @Test
  public void register_ignores_null_login_or_password() {
    TestPasswords.register("login-without-password", null);
    TestPasswords.register(null, "A-Passw0rd-without-login");

    assertThat(TestPasswords.of("login-without-password")).isEqualTo("login-without-password");
  }
}
