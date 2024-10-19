/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.auth.github;

import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.System2;

import static org.assertj.core.api.Assertions.assertThat;

public class UserIdentityFactoryImplTest {


  private MapSettings settings = new MapSettings(new PropertyDefinitions(System2.INSTANCE, GitHubSettings.definitions()));
  private UserIdentityFactoryImpl underTest = new UserIdentityFactoryImpl();

  @Test
  public void no_email() {
    GsonUser gson = new GsonUser("ABCD", "octocat", "monalisa octocat", null);

    UserIdentity identity = underTest.create(gson, null, null);

    assertThat(identity.getProviderLogin()).isEqualTo("octocat");
    assertThat(identity.getName()).isEqualTo("monalisa octocat");
    assertThat(identity.getEmail()).isNull();
  }

  @Test
  public void empty_name_is_replaced_by_provider_login() {
    GsonUser gson = new GsonUser("ABCD", "octocat", "", "octocat@github.com");

    UserIdentity identity = underTest.create(gson, null, null);

    assertThat(identity.getName()).isEqualTo("octocat");
  }

  @Test
  public void null_name_is_replaced_by_provider_login() {
    GsonUser gson = new GsonUser("ABCD", "octocat", null, "octocat@github.com");

    UserIdentity identity = underTest.create(gson, null, null);

    assertThat(identity.getName()).isEqualTo("octocat");
  }
}
