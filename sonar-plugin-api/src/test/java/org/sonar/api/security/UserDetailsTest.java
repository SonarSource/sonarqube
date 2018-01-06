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
package org.sonar.api.security;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserDetailsTest {
  private UserDetails userDetails;

  @Before
  public void init() {
    userDetails = new UserDetails();
  }

  @Test
  public void getNameTest() {
    userDetails.setName(null);
    assertThat(userDetails.getName()).isNull();

    userDetails.setName("");
    assertThat(userDetails.getName()).isEqualTo("");

    userDetails.setName("foo");
    assertThat(userDetails.getName()).isEqualTo("foo");
  }

  @Test
  public void getEmailTest() {
    userDetails.setEmail(null);
    assertThat(userDetails.getEmail()).isNull();

    userDetails.setEmail("");
    assertThat(userDetails.getEmail()).isEqualTo("");

    userDetails.setEmail("foo@example.com");
    assertThat(userDetails.getEmail()).isEqualTo("foo@example.com");
  }

  @Test
  public void getUserIdTest() {
    userDetails.setUserId(null);
    assertThat(userDetails.getUserId()).isNull();

    userDetails.setUserId("");
    assertThat(userDetails.getUserId()).isEqualTo("");

    userDetails.setUserId("foo@example");
    assertThat(userDetails.getUserId()).isEqualTo("foo@example");
  }

  @Test
  public void toStringTest() {
    userDetails.setName("foo");
    userDetails.setEmail("foo@example.com");
    userDetails.setUserId("foo@example");

    assertThat(userDetails.toString()).isEqualTo("UserDetails{name='foo', email='foo@example.com', userId='foo@example'}");
  }
}
