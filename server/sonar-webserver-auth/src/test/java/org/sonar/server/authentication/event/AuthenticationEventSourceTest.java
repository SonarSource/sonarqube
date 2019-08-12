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
package org.sonar.server.authentication.event;

import java.io.Serializable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.authentication.BaseIdentityProvider;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method;
import static org.sonar.server.authentication.event.AuthenticationEvent.Provider;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class AuthenticationEventSourceTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void local_fails_with_NPE_if_method_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("method can't be null");

    Source.local(null);
  }

  @Test
  public void local_creates_source_instance_with_specified_method_and_hardcoded_provider_and_provider_name() {
    Source underTest = Source.local(Method.BASIC_TOKEN);

    assertThat(underTest.getMethod()).isEqualTo(Method.BASIC_TOKEN);
    assertThat(underTest.getProvider()).isEqualTo(Provider.LOCAL);
    assertThat(underTest.getProviderName()).isEqualTo("local");
  }

  @Test
  public void oauth2_fails_with_NPE_if_provider_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("identityProvider can't be null");

    Source.oauth2(null);
  }

  @Test
  public void oauth2_fails_with_NPE_if_providerName_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("provider name can't be null");

    Source.oauth2(newOauth2IdentityProvider(null));
  }

  @Test
  public void oauth2_fails_with_IAE_if_providerName_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("provider name can't be empty");

    Source.oauth2(newOauth2IdentityProvider(""));
  }

  @Test
  public void oauth2_creates_source_instance_with_specified_provider_name_and_hardcoded_provider_and_method() {
    Source underTest = Source.oauth2(newOauth2IdentityProvider("some name"));

    assertThat(underTest.getMethod()).isEqualTo(Method.OAUTH2);
    assertThat(underTest.getProvider()).isEqualTo(Provider.EXTERNAL);
    assertThat(underTest.getProviderName()).isEqualTo("some name");
  }

  @Test
  public void realm_fails_with_NPE_if_method_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("method can't be null");

    Source.realm(null, "name");
  }

  @Test
  public void realm_fails_with_NPE_if_providerName_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("provider name can't be null");

    Source.realm(Method.BASIC, null);
  }

  @Test
  public void realm_fails_with_IAE_if_providerName_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("provider name can't be empty");

    Source.realm(Method.BASIC, "");
  }

  @Test
  public void realm_creates_source_instance_with_specified_method_and_provider_name_and_hardcoded_provider() {
    Source underTest = Source.realm(Method.BASIC, "some name");

    assertThat(underTest.getMethod()).isEqualTo(Method.BASIC);
    assertThat(underTest.getProvider()).isEqualTo(Provider.REALM);
    assertThat(underTest.getProviderName()).isEqualTo("some name");
  }

  @Test
  public void sso_returns_source_instance_with_hardcoded_method_provider_and_providerName() {
    Source underTest = Source.sso();

    assertThat(underTest.getMethod()).isEqualTo(Method.SSO);
    assertThat(underTest.getProvider()).isEqualTo(Provider.SSO);
    assertThat(underTest.getProviderName()).isEqualTo("sso");

    assertThat(underTest).isSameAs(Source.sso());
  }

  @Test
  public void jwt_returns_source_instance_with_hardcoded_method_provider_and_providerName() {
    Source underTest = Source.jwt();

    assertThat(underTest.getMethod()).isEqualTo(Method.JWT);
    assertThat(underTest.getProvider()).isEqualTo(Provider.JWT);
    assertThat(underTest.getProviderName()).isEqualTo("jwt");

    assertThat(underTest).isSameAs(Source.jwt());
  }

  @Test
  public void external_fails_with_NPE_if_provider_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("identityProvider can't be null");

    Source.external(null);
  }

  @Test
  public void external_fails_with_NPE_if_providerName_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("provider name can't be null");

    Source.external(newBasicIdentityProvider(null));
  }

  @Test
  public void external_fails_with_IAE_if_providerName_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("provider name can't be empty");

    Source.external(newBasicIdentityProvider(""));
  }

  @Test
  public void external_creates_source_instance_with_specified_provider_name_and_hardcoded_provider_and_method() {
    Source underTest = Source.external(newBasicIdentityProvider("some name"));

    assertThat(underTest.getMethod()).isEqualTo(Method.EXTERNAL);
    assertThat(underTest.getProvider()).isEqualTo(Provider.EXTERNAL);
    assertThat(underTest.getProviderName()).isEqualTo("some name");
  }

  @Test
  public void source_is_serializable() {
    assertThat(Serializable.class.isAssignableFrom(Source.class)).isTrue();
  }

  @Test
  public void toString_displays_all_fields() {
    assertThat(Source.sso().toString())
      .isEqualTo("Source{method=SSO, provider=SSO, providerName='sso'}");
    assertThat(Source.oauth2(newOauth2IdentityProvider("bou")).toString())
      .isEqualTo("Source{method=OAUTH2, provider=EXTERNAL, providerName='bou'}");
  }

  @Test
  public void source_implements_equals_on_all_fields() {
    assertThat(Source.sso()).isEqualTo(Source.sso());
    assertThat(Source.sso()).isNotEqualTo(Source.jwt());
    assertThat(Source.jwt()).isEqualTo(Source.jwt());
    assertThat(Source.local(Method.BASIC)).isEqualTo(Source.local(Method.BASIC));
    assertThat(Source.local(Method.BASIC)).isNotEqualTo(Source.local(Method.BASIC_TOKEN));
    assertThat(Source.local(Method.BASIC)).isNotEqualTo(Source.sso());
    assertThat(Source.local(Method.BASIC)).isNotEqualTo(Source.jwt());
    assertThat(Source.local(Method.BASIC)).isNotEqualTo(Source.oauth2(newOauth2IdentityProvider("voo")));
    assertThat(Source.oauth2(newOauth2IdentityProvider("foo")))
      .isEqualTo(Source.oauth2(newOauth2IdentityProvider("foo")));
    assertThat(Source.oauth2(newOauth2IdentityProvider("foo")))
      .isNotEqualTo(Source.oauth2(newOauth2IdentityProvider("bar")));
  }

  private static OAuth2IdentityProvider newOauth2IdentityProvider(String name) {
    OAuth2IdentityProvider mock = mock(OAuth2IdentityProvider.class);
    when(mock.getName()).thenReturn(name);
    return mock;
  }

  private static BaseIdentityProvider newBasicIdentityProvider(String name) {
    BaseIdentityProvider mock = mock(BaseIdentityProvider.class);
    when(mock.getName()).thenReturn(name);
    return mock;
  }
}
