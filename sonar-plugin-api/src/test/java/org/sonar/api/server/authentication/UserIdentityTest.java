/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class UserIdentityTest {

  @Rule
  public ExpectedException thrown= ExpectedException.none();

  @Test
  public void create_user() throws Exception {
    UserIdentity underTest = UserIdentity.builder()
      .setId("john")
      .setName("John")
      .setEmail("john@email.com")
      .build();

    assertThat(underTest.getId()).isEqualTo("john");
    assertThat(underTest.getName()).isEqualTo("John");
    assertThat(underTest.getEmail()).isEqualTo("john@email.com");
  }

  @Test
  public void create_user_without_email() throws Exception {
    UserIdentity underTest = UserIdentity.builder()
      .setId("john")
      .setName("John")
      .build();

    assertThat(underTest.getEmail()).isNull();
  }

  @Test
  public void fail_when_id_is_null() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("User id must not be blank");
    UserIdentity.builder()
      .setName("John")
      .setEmail("john@email.com")
      .build();
  }

  @Test
  public void fail_when_id_is_empty() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("User id must not be blank");
    UserIdentity.builder()
      .setId("")
      .setName("John")
      .setEmail("john@email.com")
      .build();
  }

  @Test
  public void fail_when_id_is_loo_long() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("User id size is incorrect (Between 3 and 255 characters)");
    UserIdentity.builder()
      .setId(Strings.repeat("1", 256))
      .setName("John")
      .setEmail("john@email.com")
      .build();
  }

  @Test
  public void fail_when_id_is_loo_small() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("User id size is incorrect (Between 3 and 255 characters)");
    UserIdentity.builder()
      .setId("ab")
      .setName("John")
      .setEmail("john@email.com")
      .build();
  }

  @Test
  public void fail_when_name_is_null() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("User name must not be blank");
    UserIdentity.builder()
      .setId("john")
      .setEmail("john@email.com")
      .build();
  }

  @Test
  public void fail_when_name_is_empty() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("User name must not be blank");
    UserIdentity.builder()
      .setId("john")
      .setName("")
      .setEmail("john@email.com")
      .build();
  }

  @Test
  public void fail_when_name_is_loo_long() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("User name size is too big (200 characters max)");
    UserIdentity.builder()
      .setId("john")
      .setName(Strings.repeat("1", 201))
      .setEmail("john@email.com")
      .build();
  }

  @Test
  public void fail_when_email_is_loo_long() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("User email size is too big (100 characters max)");
    UserIdentity.builder()
      .setId("john")
      .setName("John")
      .setEmail(Strings.repeat("1", 101))
      .build();
  }
}
