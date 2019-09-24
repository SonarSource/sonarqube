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
package org.sonar.auth.github;

import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.authentication.UserIdentity;

import static org.assertj.core.api.Assertions.assertThat;

public class UserIdentityFactoryImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapSettings settings = new MapSettings(new PropertyDefinitions(GitHubSettings.definitions()));
  private UserIdentityFactoryImpl underTest = new UserIdentityFactoryImpl(new GitHubSettings(settings.asConfig()));

  /**
   * Keep the same login as at GitHub
   */
  @Test
  public void create_for_provider_strategy() {
    GsonUser gson = new GsonUser("ABCD", "octocat", "monalisa octocat", "octocat@github.com");
    settings.setProperty(GitHubSettings.LOGIN_STRATEGY, GitHubSettings.LOGIN_STRATEGY_PROVIDER_ID);

    UserIdentity identity = underTest.create(gson, gson.getEmail(), null);

    assertThat(identity.getProviderId()).isEqualTo("ABCD");
    assertThat(identity.getLogin()).isEqualTo("octocat");
    assertThat(identity.getName()).isEqualTo("monalisa octocat");
    assertThat(identity.getEmail()).isEqualTo("octocat@github.com");
  }

  @Test
  public void no_email() {
    GsonUser gson = new GsonUser("ABCD", "octocat", "monalisa octocat", null);
    settings.setProperty(GitHubSettings.LOGIN_STRATEGY, GitHubSettings.LOGIN_STRATEGY_PROVIDER_ID);

    UserIdentity identity = underTest.create(gson, null, null);

    assertThat(identity.getLogin()).isEqualTo("octocat");
    assertThat(identity.getName()).isEqualTo("monalisa octocat");
    assertThat(identity.getEmail()).isNull();
  }

  @Test
  public void create_for_provider_strategy_with_teams() {
    GsonUser gson = new GsonUser("ABCD", "octocat", "monalisa octocat", "octocat@github.com");
    List<GsonTeam> teams = Arrays.asList(
      new GsonTeam("developers", new GsonTeam.GsonOrganization("SonarSource")));
    settings.setProperty(GitHubSettings.LOGIN_STRATEGY, GitHubSettings.LOGIN_STRATEGY_PROVIDER_ID);

    UserIdentity identity = underTest.create(gson, null, teams);

    assertThat(identity.getGroups()).containsOnly("SonarSource/developers");
  }

  @Test
  public void create_for_unique_login_strategy() {
    GsonUser gson = new GsonUser("ABCD", "octocat", "monalisa octocat", "octocat@github.com");
    settings.setProperty(GitHubSettings.LOGIN_STRATEGY, GitHubSettings.LOGIN_STRATEGY_UNIQUE);

    UserIdentity identity = underTest.create(gson, null, null);

    assertThat(identity.getLogin()).isEqualTo("octocat@github");
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

  @Test
  public void throw_ISE_if_strategy_is_not_supported() {
    settings.setProperty(GitHubSettings.LOGIN_STRATEGY, "xxx");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Login strategy not supported : xxx");

    underTest.create(new GsonUser("ABCD", "octocat", "octocat", "octocat@github.com"), null, null);
  }
}
