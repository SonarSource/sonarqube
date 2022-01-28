/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.api.server.authentication;

import com.google.common.base.Strings;
import org.junit.Test;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserIdentityTest {

  @Test
  public void create_user() {
    UserIdentity underTest = UserIdentity.builder()
      .setProviderId("4321")
      .setProviderLogin("john")
      .setName("John")
      .setEmail("john@email.com")
      .build();

    assertThat(underTest.getProviderId()).isEqualTo("4321");
    assertThat(underTest.getProviderLogin()).isEqualTo("john");
    assertThat(underTest.getName()).isEqualTo("John");
    assertThat(underTest.getEmail()).isEqualTo("john@email.com");
    assertThat(underTest.shouldSyncGroups()).isFalse();
    assertThat(underTest.getGroups()).isEmpty();
  }

  @Test
  public void create_user_with_minimum_fields() {
    UserIdentity underTest = UserIdentity.builder()
      .setProviderLogin("john")
      .setName("John")
      .build();

    assertThat(underTest.getProviderLogin()).isEqualTo("john");
    assertThat(underTest.getName()).isEqualTo("John");
    assertThat(underTest.getProviderId()).isNull();
    assertThat(underTest.getEmail()).isNull();
    assertThat(underTest.shouldSyncGroups()).isFalse();
    assertThat(underTest.getGroups()).isEmpty();
  }

  @Test
  public void fail_when_id_is_too_long() {
    assertThatThrownBy(() -> UserIdentity.builder()
      .setProviderId(Strings.repeat("1", 256))
      .setProviderLogin("john")
      .setName("John")
      .build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("ID is too big (255 characters max)");
  }

  @Test
  public void fail_when_provider_login_is_null() {
    assertThatThrownBy(() -> UserIdentity.builder()
      .setName("John")
      .setEmail("john@email.com")
      .build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Provider login must not be blank");
  }

  @Test
  public void fail_when_provider_login_is_empty() {
    assertThatThrownBy(() -> UserIdentity.builder()
      .setProviderLogin("")
      .setName("John")
      .setEmail("john@email.com")
      .build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Provider login must not be blank");
  }

  @Test
  public void fail_when_provider_login_is_too_long() {
    assertThatThrownBy(() -> UserIdentity.builder()
      .setProviderLogin(Strings.repeat("1", 256))
      .setName("John")
      .setEmail("john@email.com")
      .build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Provider login size is incorrect (maximum 255 characters)");
  }

  @Test
  public void fail_when_name_is_null() {
    assertThatThrownBy(() -> UserIdentity.builder()
      .setProviderLogin("john")
      .setEmail("john@email.com")
      .build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("User name must not be blank");
  }

  @Test
  public void fail_when_name_is_empty() {
    assertThatThrownBy(() -> UserIdentity.builder()
      .setProviderLogin("john")
      .setName("")
      .setEmail("john@email.com")
      .build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("User name must not be blank");
  }

  @Test
  public void fail_when_name_is_loo_long() {
    assertThatThrownBy(() -> UserIdentity.builder()
      .setProviderLogin("john")
      .setName(Strings.repeat("1", 201))
      .setEmail("john@email.com")
      .build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("User name size is too big (200 characters max)");
  }

  @Test
  public void fail_when_email_is_loo_long() {
    assertThatThrownBy(() -> UserIdentity.builder()
      .setProviderLogin("john")
      .setName("John")
      .setEmail(Strings.repeat("1", 101))
      .build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("User email size is too big (100 characters max)");
  }

  @Test
  public void create_user_with_groups() {
    UserIdentity underTest = UserIdentity.builder()
      .setProviderLogin("john")
      .setName("John")
      .setEmail("john@email.com")
      .setGroups(newHashSet("admin", "user"))
      .build();

    assertThat(underTest.shouldSyncGroups()).isTrue();
    assertThat(underTest.getGroups()).containsOnly("admin", "user");
  }

  @Test
  public void fail_when_groups_are_null() {
    assertThatThrownBy(() -> UserIdentity.builder()
      .setProviderLogin("john")
      .setEmail("john@email.com")
      .setGroups(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Groups cannot be null, please don't use this method if groups should not be synchronized.");
  }

  @Test
  public void fail_when_groups_contain_empty_group_name() {
    assertThatThrownBy(() -> UserIdentity.builder()
      .setProviderLogin("john")
      .setEmail("john@email.com")
      .setGroups(newHashSet("")))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Group name cannot be empty");
  }

  @Test
  public void fail_when_groups_contain_only_blank_space() {
    assertThatThrownBy(() -> UserIdentity.builder()
      .setProviderLogin("john")
      .setName("John")
      .setEmail("john@email.com")
      .setGroups(newHashSet("      ")))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Group name cannot be empty");
  }

  @Test
  public void fail_when_groups_contain_null_group_name() {
    assertThatThrownBy(() -> UserIdentity.builder()
      .setProviderLogin("john")
      .setName("John")
      .setEmail("john@email.com")
      .setGroups(newHashSet((String)null)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Group name cannot be empty");
  }

  @Test
  public void fail_when_groups_contain_anyone() {
    assertThatThrownBy(() -> UserIdentity.builder()
      .setProviderLogin("john")
      .setName("John")
      .setEmail("john@email.com")
      .setGroups(newHashSet("Anyone")))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Anyone group cannot be used");
  }

  @Test
  public void fail_when_groups_contain_too_long_group_name() {
    assertThatThrownBy(() -> UserIdentity.builder()
      .setProviderLogin("john")
      .setName("John")
      .setEmail("john@email.com")
      .setGroups(newHashSet(Strings.repeat("group", 300))))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Group name cannot be longer than 255 characters");
  }

}
