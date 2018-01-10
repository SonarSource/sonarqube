/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.user;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class UserDtoTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void encode_scm_accounts() {
    assertThat(UserDto.encodeScmAccounts(null)).isNull();
    assertThat(UserDto.encodeScmAccounts(Collections.emptyList())).isNull();
    assertThat(UserDto.encodeScmAccounts(Arrays.asList("foo"))).isEqualTo("\nfoo\n");
    assertThat(UserDto.encodeScmAccounts(Arrays.asList("foo", "bar"))).isEqualTo("\nfoo\nbar\n");
  }

  @Test
  public void decode_scm_accounts() {
    assertThat(UserDto.decodeScmAccounts(null)).isEmpty();
    assertThat(UserDto.decodeScmAccounts("\nfoo\n")).containsOnly("foo");
    assertThat(UserDto.decodeScmAccounts("\nfoo\nbar\n")).containsOnly("foo", "bar");
  }

  @Test
  public void encrypt_password() {
    assertThat(UserDto.encryptPassword("PASSWORD", "0242b0b4c0a93ddfe09dd886de50bc25ba000b51")).isEqualTo("540e4fc4be4e047db995bc76d18374a5b5db08cc");
  }

  @Test
  public void fail_to_encrypt_password_when_password_is_null() {
    expectedException.expect(NullPointerException.class);
    UserDto.encryptPassword(null, "salt");
  }

  @Test
  public void fail_to_encrypt_password_when_salt_is_null() {
    expectedException.expect(NullPointerException.class);
    UserDto.encryptPassword("password", null);
  }
}
