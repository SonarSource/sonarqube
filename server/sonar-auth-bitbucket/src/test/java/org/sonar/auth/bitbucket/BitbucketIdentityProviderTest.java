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
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BitbucketIdentityProviderTest {


  private final MapSettings settings = new MapSettings();
  private final BitbucketSettings bitbucketSettings = new BitbucketSettings(settings.asConfig());
  private final UserIdentityFactory userIdentityFactory = mock(UserIdentityFactory.class);
  private final BitbucketScribeApi scribeApi = new BitbucketScribeApi(bitbucketSettings);
  private final BitbucketIdentityProvider underTest = new BitbucketIdentityProvider(bitbucketSettings, userIdentityFactory, scribeApi);

  @Test
  public void check_fields() {
    assertThat(underTest.getKey()).isEqualTo("bitbucket");
    assertThat(underTest.getName()).isEqualTo("Bitbucket");
    assertThat(underTest.getDisplay().getIconPath()).isEqualTo("/images/alm/bitbucket-white.svg");
    assertThat(underTest.getDisplay().getBackgroundColor()).isEqualTo("#0052cc");
  }

  @Test
  public void is_enabled() {
    enableBitbucketAuthentication(true);
    assertThat(underTest.isEnabled()).isTrue();

    settings.setProperty("sonar.auth.bitbucket.enabled", false);
    assertThat(underTest.isEnabled()).isFalse();
  }

  @Test
  public void init() {
    enableBitbucketAuthentication(true);
    OAuth2IdentityProvider.InitContext context = mock(OAuth2IdentityProvider.InitContext.class);
    when(context.generateCsrfState()).thenReturn("state");
    when(context.getCallbackUrl()).thenReturn("http://localhost/callback");

    underTest.init(context);

    verify(context).redirectTo("https://bitbucket.org/site/oauth2/authorize?response_type=code&client_id=id&redirect_uri=http%3A%2F%2Flocalhost%2Fcallback&scope=account&state=state");
  }

  @Test
  public void fail_to_init_when_disabled() {
    enableBitbucketAuthentication(false);
    OAuth2IdentityProvider.InitContext context = mock(OAuth2IdentityProvider.InitContext.class);

    assertThatThrownBy(() -> underTest.init(context))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Bitbucket authentication is disabled");
  }

  private void enableBitbucketAuthentication(boolean enabled) {
    if (enabled) {
      settings.setProperty("sonar.auth.bitbucket.clientId.secured", "id");
      settings.setProperty("sonar.auth.bitbucket.clientSecret.secured", "secret");
      settings.setProperty("sonar.auth.bitbucket.enabled", true);
    } else {
      settings.setProperty("sonar.auth.bitbucket.enabled", false);
    }
  }

}
