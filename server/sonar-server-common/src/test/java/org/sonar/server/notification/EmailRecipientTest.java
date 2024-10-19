/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.notification;

import org.junit.Test;
import org.sonar.server.notification.NotificationManager.EmailRecipient;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EmailRecipientTest {

  @Test
  public void constructor_fails_with_NPE_if_login_is_null() {
    String email = randomAlphabetic(12);

    assertThatThrownBy(() -> new EmailRecipient(null, email))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("login can't be null");
  }

  @Test
  public void constructor_fails_with_NPE_if_email_is_null() {
    String login = randomAlphabetic(12);

    assertThatThrownBy(() -> new EmailRecipient(login, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("email can't be null");
  }

  @Test
  public void equals_is_based_on_login_and_email() {
    String login = randomAlphabetic(11);
    String email = randomAlphabetic(12);
    EmailRecipient underTest = new EmailRecipient(login, email);

    assertThat(underTest)
      .isEqualTo(new EmailRecipient(login, email))
      .isNotNull()
      .isNotEqualTo(new Object())
      .isNotEqualTo(new EmailRecipient(email, login))
      .isNotEqualTo(new EmailRecipient(randomAlphabetic(5), email))
      .isNotEqualTo(new EmailRecipient(login, randomAlphabetic(5)))
      .isNotEqualTo(new EmailRecipient(randomAlphabetic(5), randomAlphabetic(6)));
  }

  @Test
  public void hashcode_is_based_on_login_and_email() {
    String login = randomAlphabetic(11);
    String email = randomAlphabetic(12);
    EmailRecipient underTest = new EmailRecipient(login, email);

    assertThat(underTest.hashCode())
      .isEqualTo(new EmailRecipient(login, email).hashCode())
      .isNotEqualTo(new Object().hashCode())
      .isNotEqualTo(new EmailRecipient(email, login).hashCode())
      .isNotEqualTo(new EmailRecipient(randomAlphabetic(5), email).hashCode())
      .isNotEqualTo(new EmailRecipient(login, randomAlphabetic(5)).hashCode())
      .isNotEqualTo(new EmailRecipient(randomAlphabetic(5), randomAlphabetic(6)).hashCode());
  }

  @Test
  public void verify_to_String() {
    String login = randomAlphabetic(11);
    String email = randomAlphabetic(12);

    assertThat(new EmailRecipient(login, email)).hasToString("EmailRecipient{'" + login + "':'" + email + "'}");
  }
}
