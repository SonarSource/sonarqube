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
package org.sonar.server.issue.notification;

import org.junit.Test;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class EmailMessageTest {
  private EmailMessage underTest = new EmailMessage();

  @Test
  public void setHtmlMessage_sets_message_and_html_to_true() {
    String message = randomAlphabetic(12);

    underTest.setHtmlMessage(message);

    assertThat(underTest.getMessage()).isEqualTo(message);
    assertThat(underTest.isHtml()).isTrue();
  }

  @Test
  public void setPlainTextMessage_sets_message_and_html_to_false() {
    String message = randomAlphabetic(12);

    underTest.setPlainTextMessage(message);

    assertThat(underTest.getMessage()).isEqualTo(message);
    assertThat(underTest.isHtml()).isFalse();
  }
}
