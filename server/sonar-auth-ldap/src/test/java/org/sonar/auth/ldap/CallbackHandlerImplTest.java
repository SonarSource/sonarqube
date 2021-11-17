/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class CallbackHandlerImplTest {

  @Test
  public void test() throws Exception {
    NameCallback nameCallback = new NameCallback("username");
    PasswordCallback passwordCallback = new PasswordCallback("password", false);
    new CallbackHandlerImpl("tester", "secret").handle(new Callback[] {nameCallback, passwordCallback});

    assertThat(nameCallback.getName()).isEqualTo("tester");
    assertThat(passwordCallback.getPassword()).isEqualTo("secret".toCharArray());
  }

  @Test
  public void unsupportedCallback() {
    assertThatThrownBy(() -> {
      new CallbackHandlerImpl("tester", "secret").handle(new Callback[] {mock(Callback.class)});
    })
      .isInstanceOf(UnsupportedCallbackException.class);
  }

}
