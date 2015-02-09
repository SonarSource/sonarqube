/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.user;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class UserDtoTest {

  @Test
  public void encode_scm_accounts() throws Exception {
    assertThat(UserDto.encodeScmAccounts(null)).isNull();
    assertThat(UserDto.encodeScmAccounts(Collections.<String>emptyList())).isNull();
    assertThat(UserDto.encodeScmAccounts(Arrays.asList("foo"))).isEqualTo("\nfoo\n");
    assertThat(UserDto.encodeScmAccounts(Arrays.asList("foo", "bar"))).isEqualTo("\nfoo\nbar\n");
  }

  @Test
  public void decode_scm_accounts() throws Exception {
    assertThat(UserDto.decodeScmAccounts(null)).isEmpty();
    assertThat(UserDto.decodeScmAccounts("\nfoo\n")).containsOnly("foo");
    assertThat(UserDto.decodeScmAccounts("\nfoo\nbar\n")).containsOnly("foo", "bar");
  }
}
