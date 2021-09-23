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
package org.sonar.auth.bitbucket;

import org.junit.Test;
import org.sonar.api.server.authentication.UserIdentity;

import static org.assertj.core.api.Assertions.assertThat;

public class UserIdentityFactoryTest {

  private UserIdentityFactory underTest = new UserIdentityFactory();

  @Test
  public void create_login() {
    GsonUser gson = new GsonUser("john", "John", "ABCD");
    UserIdentity identity = underTest.create(gson, null);
    assertThat(identity.getName()).isEqualTo("John");
    assertThat(identity.getEmail()).isNull();
    assertThat(identity.getProviderId()).isEqualTo("ABCD");
  }

  @Test
  public void empty_name_is_replaced_by_provider_login() {
    GsonUser gson = new GsonUser("john", "", "ABCD");

    UserIdentity identity = underTest.create(gson, null);
    assertThat(identity.getName()).isEqualTo("john");
  }

  @Test
  public void null_name_is_replaced_by_provider_login() {
    GsonUser gson = new GsonUser("john", null, "ABCD");

    UserIdentity identity = underTest.create(gson, null);
    assertThat(identity.getName()).isEqualTo("john");
  }

}
