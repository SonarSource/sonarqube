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
package org.sonar.auth.saml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.server.authentication.UserIdentity;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrincipalToUserIdentityConverterTest {

  @Mock
  private SamlSettings samlSettings;

  @InjectMocks
  private PrincipalToUserIdentityConverter converter;

  @BeforeEach
  public void setUp() {
    lenient().when(samlSettings.getUserLogin()).thenReturn("login");
    lenient().when(samlSettings.getUserName()).thenReturn("name");
    lenient().when(samlSettings.getUserEmail()).thenReturn(Optional.of("email"));
    lenient().when(samlSettings.getGroupName()).thenReturn(Optional.of("group"));
  }

  @Test
  void convertToUserIdentity_whenLoginIsNull_throws() {
    Saml2AuthenticatedPrincipal principal = mock();
    assertThatIllegalStateException()
      .isThrownBy(() -> converter.convertToUserIdentity(principal))
      .withMessage("%s is missing".formatted(samlSettings.getUserLogin()));
  }

  @Test
  void convertToUserIdentity_whenNameIsNull_throws() {
    Saml2AuthenticatedPrincipal principal = mock();
    when(principal.getFirstAttribute("login")).thenReturn("mylogin");

    assertThatIllegalStateException()
      .isThrownBy(() -> converter.convertToUserIdentity(principal))
      .withMessage("%s is missing".formatted(samlSettings.getUserName()));
  }

  @Test
  void convertToUserIdentity_whenOnlyLoginAndNameSettingConfigured_succeeds() {
    when(samlSettings.getUserEmail()).thenReturn(Optional.empty());
    when(samlSettings.getGroupName()).thenReturn(Optional.empty());

    Saml2AuthenticatedPrincipal principal = new DefaultSaml2AuthenticatedPrincipal("unused", Map.of("name", List.of("myname"), "login", List.of("mylogin")));

    UserIdentity userIdentity = converter.convertToUserIdentity(principal);

    assertThat(userIdentity.getProviderLogin()).isEqualTo("mylogin");
    assertThat(userIdentity.getName()).isEqualTo("myname");
    assertThat(userIdentity.getEmail()).isNull();
    assertThat(userIdentity.getGroups()).isEmpty();
  }

  @Test
  void convertToUserIdentity_whenAllSettingConfiguredButNoValue_succeeds() {
    Saml2AuthenticatedPrincipal principal = new DefaultSaml2AuthenticatedPrincipal("unused", Map.of("name", List.of("myname"), "login", List.of("mylogin")));

    UserIdentity userIdentity = converter.convertToUserIdentity(principal);

    assertThat(userIdentity.getProviderLogin()).isEqualTo("mylogin");
    assertThat(userIdentity.getName()).isEqualTo("myname");
    assertThat(userIdentity.getEmail()).isNull();
    assertThat(userIdentity.getGroups()).isEmpty();
  }

  @Test
  void convertToUserIdentity_whenAllSettingConfiguredAndNullValuesForEmailAndGroup_succeeds() {
    List<Object> listWithNull = new ArrayList<>();
    listWithNull.add(null);

    Saml2AuthenticatedPrincipal principal = buildPrincipal("myname", "mylogin", listWithNull, listWithNull);

    UserIdentity userIdentity = converter.convertToUserIdentity(principal);

    assertThat(userIdentity.getProviderLogin()).isEqualTo("mylogin");
    assertThat(userIdentity.getName()).isEqualTo("myname");
    assertThat(userIdentity.getEmail()).isNull();
    assertThat(userIdentity.getGroups()).isEmpty();
  }

  @Test
  void convertToUserIdentity_whenAllSettingConfiguredAndEmptyValueForEmailAndGroup_succeeds() {
    Saml2AuthenticatedPrincipal principal = buildPrincipal("myname", "mylogin", List.of(), List.of());

    UserIdentity userIdentity = converter.convertToUserIdentity(principal);

    assertThat(userIdentity.getProviderLogin()).isEqualTo("mylogin");
    assertThat(userIdentity.getName()).isEqualTo("myname");
    assertThat(userIdentity.getEmail()).isNull();
    assertThat(userIdentity.getGroups()).isEmpty();
  }

  @Test
  void convertToUserIdentity_whenAllSettingConfiguredAndValuePresent_succeeds() {
    Saml2AuthenticatedPrincipal principal = buildPrincipal("myname", "mylogin", List.of("myemail"), List.of("mygroup"));

    UserIdentity userIdentity = converter.convertToUserIdentity(principal);

    assertThat(userIdentity.getProviderLogin()).isEqualTo("mylogin");
    assertThat(userIdentity.getName()).isEqualTo("myname");
    assertThat(userIdentity.getEmail()).isEqualTo("myemail");
    assertThat(userIdentity.getGroups()).containsExactly("mygroup");
  }

  @Test
  void convertToUserIdentity_whenAllSettingConfiguredAndMultipleEmails_takesFirstOne() {
    Saml2AuthenticatedPrincipal principal = buildPrincipal("myname", "mylogin", List.of("myemail", "myemail2"), List.of());

    UserIdentity userIdentity = converter.convertToUserIdentity(principal);

    assertThat(userIdentity.getProviderLogin()).isEqualTo("mylogin");
    assertThat(userIdentity.getName()).isEqualTo("myname");
    assertThat(userIdentity.getEmail()).isEqualTo("myemail");
  }

  @Test
  void convertToUserIdentity_whenAllSettingConfiguredAndMultipleGroups_mapsThemAll() {
    Saml2AuthenticatedPrincipal principal = buildPrincipal("myname", "mylogin", List.of(), List.of("group1", "group2"));

    UserIdentity userIdentity = converter.convertToUserIdentity(principal);

    assertThat(userIdentity.getProviderLogin()).isEqualTo("mylogin");
    assertThat(userIdentity.getName()).isEqualTo("myname");
    assertThat(userIdentity.getGroups()).containsExactlyInAnyOrder("group1", "group2");
  }

  private Saml2AuthenticatedPrincipal buildPrincipal(String name, String login, List<Object> emails, List<Object> groups) {
    return new DefaultSaml2AuthenticatedPrincipal("unused",
      Map.of("name", List.of(name),
        "login", List.of(login),
        "email", emails,
        "group", groups)
    );
  }

}
