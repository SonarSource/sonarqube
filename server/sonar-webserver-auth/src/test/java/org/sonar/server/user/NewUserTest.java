/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.user;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class NewUserTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_new_user() {
    NewUser newUser = NewUser.builder()
      .setLogin("login")
      .setName("name")
      .setEmail("email")
      .setPassword("password")
      .setScmAccounts(asList("login1", "login2"))
      .build();

    assertThat(newUser.login()).isEqualTo("login");
    assertThat(newUser.name()).isEqualTo("name");
    assertThat(newUser.email()).isEqualTo("email");
    assertThat(newUser.password()).isEqualTo("password");
    assertThat(newUser.scmAccounts()).contains("login1", "login2");
    assertThat(newUser.externalIdentity()).isNull();
  }

  @Test
  public void create_new_user_with_minimal_fields() {
    NewUser newUser = NewUser.builder().build();

    assertThat(newUser.login()).isNull();
    assertThat(newUser.name()).isNull();
    assertThat(newUser.email()).isNull();
    assertThat(newUser.password()).isNull();
    assertThat(newUser.scmAccounts()).isEmpty();
  }

  @Test
  public void create_new_user_with_authority() {
    NewUser newUser = NewUser.builder()
      .setLogin("login")
      .setName("name")
      .setEmail("email")
      .setExternalIdentity(new ExternalIdentity("github", "github_login", "ABCD"))
      .build();

    assertThat(newUser.externalIdentity().getProvider()).isEqualTo("github");
    assertThat(newUser.externalIdentity().getLogin()).isEqualTo("github_login");
    assertThat(newUser.externalIdentity().getId()).isEqualTo("ABCD");
  }

  @Test
  public void fail_to_set_password_when_external_identity_is_set() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Password should not be set with an external identity");

    NewUser.builder()
      .setLogin("login")
      .setName("name")
      .setEmail("email")
      .setPassword("password")
      .setExternalIdentity(new ExternalIdentity("github", "github_login", "ABCD"))
      .build();
  }
}
