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
package org.sonar.server.issue.notification;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonar.api.notifications.Notification;

class MQRAndStandardModesExistTemplateTest {

  private final MQRAndStandardModesExistTemplate underTest = new MQRAndStandardModesExistTemplate();

  @Test
  void format_whenStandardExperience_shouldReturnExpectEmailMessage() {
    Assertions.assertThat(underTest.format(new MQRAndStandardModesExistNotification(false)))
      .extracting(EmailMessage::getSubject, EmailMessage::getMessage)
      .containsExactly("Your SonarQube instance is in Standard Experience",
        """
          In this version of SonarQube, there are two options to reflect the health of all the projects: Multi-Quality Rule (MQR) Mode and Standard Experience.
          The SonarQube documentation explains more.

          Your instance is currently using the Standard Experience.

          To change it, go to Administration > Configuration > General Settings > Mode.
          """);
  }

  @Test
  void format_whenMQRMode_shouldReturnExpectEmailMessage() {
    Assertions.assertThat(underTest.format(new MQRAndStandardModesExistNotification(true)))
      .extracting(EmailMessage::getSubject, EmailMessage::getMessage)
      .containsExactly("Your SonarQube instance is in Multi-Quality Rule (MQR) Mode",
        """
          In this version of SonarQube, there are two options to reflect the health of all the projects: Multi-Quality Rule (MQR) Mode and Standard Experience.
          The SonarQube documentation explains more.

          Your instance is currently using the Multi-Quality Rule (MQR) Mode.

          To change it, go to Administration > Configuration > General Settings > Mode.
          """);
  }

  @Test
  void format_whenInvalidNotification_shouldReturnNull() {
    Assertions.assertThat(underTest.format(new TestNotification())).isNull();
  }

  private static class TestNotification extends Notification {
    public TestNotification() {
      super("test");
    }
  }
}
