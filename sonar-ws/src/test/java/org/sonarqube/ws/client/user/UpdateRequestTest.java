/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

public class UpdateRequestTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  UpdateRequest.Builder underTest = UpdateRequest.builder();

  @Test
  public void create_request() {
    UpdateRequest result = underTest
      .setLogin("john")
      .setName("John")
      .setEmail("john@doo.com")
      .setScmAccounts(asList("jo", "hn"))
      .build();

    assertThat(result.getLogin()).isEqualTo("john");
    assertThat(result.getName()).isEqualTo("John");
    assertThat(result.getEmail()).isEqualTo("john@doo.com");
    assertThat(result.getScmAccounts()).containsOnly("jo", "hn");
  }

  @Test
  public void scm_accounts_is_empty_by_default() throws Exception {
    UpdateRequest result = underTest
      .setLogin("john")
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
      .setName("John")
      .build();
  }

}
