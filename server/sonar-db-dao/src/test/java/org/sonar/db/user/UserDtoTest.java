/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class UserDtoTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void get_and_set_scm_accounts_as_string() {
    assertThat(new UserDto().setScmAccounts((String) null).getScmAccounts()).isNull();
    assertThat(new UserDto().setScmAccounts("foo").getScmAccounts()).isEqualTo("foo");
    assertThat(new UserDto().setScmAccounts("\nfoo\nbar\n").getScmAccounts()).isEqualTo("\nfoo\nbar\n");
  }

  @Test
  public void get_and_set_scm_accounts_as_list() {
    assertThat(new UserDto().setScmAccounts((List<String>) null).getScmAccountsAsList()).isEmpty();
    assertThat(new UserDto().setScmAccounts(Collections.<String>emptyList()).getScmAccountsAsList()).isEmpty();
    assertThat(new UserDto().setScmAccounts(singletonList("foo")).getScmAccountsAsList()).containsExactlyInAnyOrder("foo");
    assertThat(new UserDto().setScmAccounts(asList("foo", "bar")).getScmAccountsAsList()).containsExactlyInAnyOrder("foo", "bar");
    assertThat(new UserDto().setScmAccounts("\nfoo\nbar\n").getScmAccountsAsList()).containsExactlyInAnyOrder("foo", "bar");
  }

  @Test
  public void get_and_set_secondary_emails_as_string() {
    assertThat(new UserDto().setSecondaryEmails((String) null).getSecondaryEmails()).isNull();
    assertThat(new UserDto().setSecondaryEmails("foo").getSecondaryEmails()).isEqualTo("foo");
    assertThat(new UserDto().setSecondaryEmails("\nfoo\nbar\n").getSecondaryEmails()).isEqualTo("\nfoo\nbar\n");
  }

  @Test
  public void get_and_set_secondary_emails_as_list() {
    assertThat(new UserDto().setSecondaryEmails((List<String>) null).getSecondaryEmailsAsList()).isEmpty();
    assertThat(new UserDto().setSecondaryEmails(Collections.<String>emptyList()).getSecondaryEmailsAsList()).isEmpty();
    assertThat(new UserDto().setSecondaryEmails(singletonList("foo")).getSecondaryEmailsAsList()).containsExactlyInAnyOrder("foo");
    assertThat(new UserDto().setSecondaryEmails(asList("foo", "bar")).getSecondaryEmailsAsList()).containsExactlyInAnyOrder("foo", "bar");
    assertThat(new UserDto().setSecondaryEmails("\nfoo\nbar\n").getSecondaryEmailsAsList()).containsExactlyInAnyOrder("foo", "bar");
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
