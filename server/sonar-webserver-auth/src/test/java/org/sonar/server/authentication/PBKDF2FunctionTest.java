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
package org.sonar.server.authentication;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PBKDF2FunctionTest {

  private static final int GENERATION_ITERATIONS = 1000;

  private final CredentialsLocalAuthentication.PBKDF2Function pbkdf2Function = new CredentialsLocalAuthentication.PBKDF2Function(GENERATION_ITERATIONS);

  @Test
  public void encryptPassword_returnsCorrectEncryptedPassword() {
    String encryptedPassword = pbkdf2Function.encryptPassword("salt", "test_password");
    assertThat(encryptedPassword)
      .isEqualTo("%d$%s", GENERATION_ITERATIONS, "Yz4QzaROW6N9dqr47NtsDgVJERKC3gTec4rMHonb885IVvTb6OYelaAvMXxoc5QT+4SAjiEmDKaUa2cAC9Ne8Q==");
  }

}
