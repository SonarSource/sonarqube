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
package org.sonar.server.authentication.event;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticationExceptionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void build_fails_with_NPE_if_source_is_null() {
    AuthenticationException.Builder builder = AuthenticationException.newBuilder()
        .setLogin("login")
        .setMessage("message");

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("source can't be null");

    builder.build();
  }

  @Test
  public void build_does_not_fail_if_login_is_null() {
    AuthenticationException exception = AuthenticationException.newBuilder()
        .setSource(Source.sso())
        .setMessage("message")
        .build();

    assertThat(exception.getSource()).isEqualTo(Source.sso());
    assertThat(exception.getMessage()).isEqualTo("message");
    assertThat(exception.getLogin()).isNull();
  }

  @Test
  public void build_does_not_fail_if_message_is_null() {
    AuthenticationException exception = AuthenticationException.newBuilder()
        .setSource(Source.sso())
        .setLogin("login")
        .build();

    assertThat(exception.getSource()).isEqualTo(Source.sso());
    assertThat(exception.getMessage()).isNull();
    assertThat(exception.getLogin()).isEqualTo("login");
  }

  @Test
  public void builder_set_methods_do_not_fail_if_login_is_null() {
    AuthenticationException.newBuilder().setSource(null).setLogin(null).setMessage(null);
  }

}
