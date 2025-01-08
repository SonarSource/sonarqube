/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.monitoring;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.server.authentication.IdentityProviderRepository;
import org.sonar.server.authentication.TestIdentityProvider;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.user.SecurityRealmFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.CoreProperties.CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE;

@RunWith(MockitoJUnitRunner.class)
public class CommonSystemInformationTest {
  @Mock
  private Configuration config;
  @Mock
  private IdentityProviderRepository identityProviderRepository;
  @Mock
  private ManagedInstanceService managedInstanceService;
  @Mock
  private SecurityRealmFactory securityRealmFactory;
  @InjectMocks
  private CommonSystemInformation commonSystemInformation;

  @Test
  public void getForceAuthentication_whenNotDefined_shouldUseDefault() {
    assertThat(commonSystemInformation.getForceAuthentication())
      .isEqualTo(CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE);
  }

  @Test
  public void getForceAuthentication_whenDefined_shouldBeUsed() {
    when(config.getBoolean(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY)).thenReturn(Optional.of(false));

    assertThat(commonSystemInformation.getForceAuthentication())
      .isFalse();
  }

  @Test
  public void getEnabledIdentityProviders_whenNonDefined_shouldReturnEmpty() {
    mockIdentityProviders(List.of());

    assertThat(commonSystemInformation.getEnabledIdentityProviders())
      .isEmpty();
  }

  @Test
  public void getEnabledIdentityProviders_whenDefined_shouldReturnOnlyEnabled() {
    mockIdentityProviders(List.of(
      new TestIdentityProvider().setKey("saml").setName("Okta").setEnabled(true),
      new TestIdentityProvider().setKey("github").setName("GitHub").setEnabled(true),
      new TestIdentityProvider().setKey("bitbucket").setName("BitBucket").setEnabled(false)
    ));

    assertThat(commonSystemInformation.getEnabledIdentityProviders())
      .containsExactlyInAnyOrder("Okta", "GitHub");
  }

  @Test
  public void getAllowsToSignUpEnabledIdentityProviders_whenNonDefined_shouldReturnEmpty() {
    mockIdentityProviders(List.of());

    assertThat(commonSystemInformation.getAllowsToSignUpEnabledIdentityProviders())
      .isEmpty();
  }

  @Test
  public void getAllowsToSignUpEnabledIdentityProviders_whenDefinedButInstanceManaged_shouldReturnNull() {
    mockIdentityProviders(List.of(
      new TestIdentityProvider().setKey("saml").setName("Okta").setEnabled(true).setAllowsUsersToSignUp(true),
      new TestIdentityProvider().setKey("github").setName("GitHub").setEnabled(true).setAllowsUsersToSignUp(false),
      new TestIdentityProvider().setKey("bitbucket").setName("BitBucket").setEnabled(false).setAllowsUsersToSignUp(false)
    ));
    mockManagedInstance(true);

    assertThat(commonSystemInformation.getAllowsToSignUpEnabledIdentityProviders())
      .isEmpty();
  }

  @Test
  public void getAllowsToSignUpEnabledIdentityProviders_whenDefined_shouldReturnOnlyEnabled() {
    mockIdentityProviders(List.of(
      new TestIdentityProvider().setKey("saml").setName("Okta").setEnabled(true).setAllowsUsersToSignUp(true),
      new TestIdentityProvider().setKey("github").setName("GitHub").setEnabled(true).setAllowsUsersToSignUp(false),
      new TestIdentityProvider().setKey("bitbucket").setName("BitBucket").setEnabled(false).setAllowsUsersToSignUp(false)
    ));

    assertThat(commonSystemInformation.getAllowsToSignUpEnabledIdentityProviders())
      .containsExactly("Okta");
  }

  @Test
  public void getManagedInstanceProvider_whenInstanceNotManaged_shouldReturnNull() {
    mockIdentityProviders(List.of());
    mockManagedInstance(false);

    assertThat(commonSystemInformation.getManagedInstanceProviderName())
      .isNull();
  }

  @Test
  public void getManagedInstanceProvider_whenInstanceManaged_shouldReturnName() {
    mockManagedInstance(true);

    assertThat(commonSystemInformation.getManagedInstanceProviderName())
      .isEqualTo("Provider");
  }

  @Test
  public void getExternalUserAuthentication_whenNotDefined_shouldReturnNull() {
    assertThat(commonSystemInformation.getExternalUserAuthentication())
      .isNull();
  }

  @Test
  public void getExternalUserAuthentication_whenDefined_shouldReturnName() {
    mockSecurityRealmFactory("Security Realm");

    assertThat(commonSystemInformation.getExternalUserAuthentication())
      .isEqualTo("Security Realm");
  }

  private void mockIdentityProviders(List<IdentityProvider> identityProviders) {
    when(identityProviderRepository.getAllEnabledAndSorted()).thenReturn(identityProviders);
  }

  private void mockManagedInstance(boolean managed) {
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(managed);
    when(managedInstanceService.getProviderName()).thenReturn("Provider");
  }

  private void mockSecurityRealmFactory(String name) {
    SecurityRealm securityRealm = mock(SecurityRealm.class);
    when(securityRealm.getName()).thenReturn(name);
    when(securityRealmFactory.getRealm()).thenReturn(securityRealm);
  }
}
