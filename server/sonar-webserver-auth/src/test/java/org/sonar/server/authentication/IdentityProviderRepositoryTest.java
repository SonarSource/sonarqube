/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.List;
import org.junit.Test;
import org.sonar.api.server.authentication.IdentityProvider;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IdentityProviderRepositoryTest {

  private static IdentityProvider GITHUB = new TestIdentityProvider()
    .setKey("github")
    .setName("Github")
    .setEnabled(true);

  private static IdentityProvider BITBUCKET = new TestIdentityProvider()
    .setKey("bitbucket")
    .setName("Bitbucket")
    .setEnabled(true);

  private static IdentityProvider DISABLED = new TestIdentityProvider()
    .setKey("disabled")
    .setName("Disabled")
    .setEnabled(false);

  @Test
  public void return_enabled_provider() {
    IdentityProviderRepository underTest = new IdentityProviderRepository(asList(GITHUB, BITBUCKET, DISABLED));

    assertThat(underTest.getEnabledByKey(GITHUB.getKey())).isEqualTo(GITHUB);
    assertThat(underTest.getEnabledByKey(BITBUCKET.getKey())).isEqualTo(BITBUCKET);
  }

  @Test
  public void fail_on_disabled_provider() {
    IdentityProviderRepository underTest = new IdentityProviderRepository(asList(GITHUB, BITBUCKET, DISABLED));

    assertThatThrownBy(() -> underTest.getEnabledByKey(DISABLED.getKey()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Identity provider disabled does not exist or is not enabled");
  }

  @Test
  public void fail_on_non_exist_provider() {
    IdentityProviderRepository underTest = new IdentityProviderRepository(asList(GITHUB, BITBUCKET, DISABLED));

    assertThatThrownBy(() -> underTest.getEnabledByKey("NotExist"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Identity provider NotExist does not exist or is not enabled");
  }

  @Test
  public void return_all_enabled_providers() {
    IdentityProviderRepository underTest = new IdentityProviderRepository(asList(GITHUB, BITBUCKET, DISABLED));

    List<IdentityProvider> providers = underTest.getAllEnabledAndSorted();
    assertThat(providers).containsOnly(GITHUB, BITBUCKET);
  }

  @Test
  public void return_sorted_enabled_providers() {
    IdentityProviderRepository underTest = new IdentityProviderRepository(asList(GITHUB, BITBUCKET));

    List<IdentityProvider> providers = underTest.getAllEnabledAndSorted();
    assertThat(providers).containsExactly(BITBUCKET, GITHUB);
  }

  @Test
  public void return_nothing_when_no_identity_provider() {
    IdentityProviderRepository underTest = new IdentityProviderRepository(null);

    assertThat(underTest.getAllEnabledAndSorted()).isEmpty();
  }
}
