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
package org.sonarqube.ws.client.user;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class CreateRequestTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  CreateRequest.Builder underTest = CreateRequest.builder();

  @Test
  public void create_request() {
    CreateRequest result = underTest
      .setLogin("john")
      .setPassword("123456")
      .setName("John")
      .setEmail("john@doo.com")
      .setScmAccounts(asList("jo", "hn"))
      .build();

    assertThat(result.getLogin()).isEqualTo("john");
    assertThat(result.getPassword()).isEqualTo("123456");
    assertThat(result.getName()).isEqualTo("John");
    assertThat(result.getEmail()).isEqualTo("john@doo.com");
    assertThat(result.getScmAccounts()).containsOnly("jo", "hn");
    assertThat(result.isLocal()).isTrue();
  }

  @Test
  public void create_request_for_local_user() {
    CreateRequest result = underTest
      .setLogin("john")
      .setPassword("123456")
      .setName("John")
      .setLocal(true)
      .build();

    assertThat(result.isLocal()).isTrue();
  }

  @Test
  public void create_request_for_none_local_user() {
    CreateRequest result = underTest
      .setLogin("john")
      .setName("John")
      .setLocal(false)
      .build();

    assertThat(result.isLocal()).isFalse();
  }

  @Test
  public void scm_accounts_is_empty_by_default() throws Exception {
    CreateRequest result = underTest
      .setLogin("john")
      .setPassword("123456")
      .setName("John")
      .setEmail("john@doo.com")
      .build();

    assertThat(result.getScmAccounts()).isEmpty();
  }

  @Test
  public void fail_when_empty_login() {
    expectedException.expect(IllegalArgumentException.class);
    underTest
      .setLogin("")
      .setPassword("123456")
      .setName("John")
      .build();
  }

  @Test
  public void fail_when_empty_password_on_local_user() {
    expectedException.expect(IllegalArgumentException.class);
    underTest
      .setLogin("john")
      .setPassword("")
      .setName("John")
      .build();
  }

  @Test
  public void fail_when_empty_name() {
    expectedException.expect(IllegalArgumentException.class);
    underTest
      .setLogin("john")
      .setPassword("12345")
      .setName("")
      .build();
  }

  @Test
  public void fail_when_password_is_set_on_none_local_user() {
    expectedException.expect(IllegalArgumentException.class);
    underTest
      .setLogin("john")
      .setPassword("12345")
      .setName("12345")
      .setLocal(false)
      .build();
  }

}
