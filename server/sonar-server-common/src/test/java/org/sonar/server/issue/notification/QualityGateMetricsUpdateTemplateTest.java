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
package org.sonar.server.issue.notification;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonar.api.notifications.Notification;

class QualityGateMetricsUpdateTemplateTest {

  QualityGateMetricsUpdateTemplate underTest = new QualityGateMetricsUpdateTemplate();

  @Test
  void format_whenStandardExperience_shouldReturnExpectEmailMessage() {
    Assertions.assertThat(underTest.format(new QualityGateMetricsUpdateNotification(false)))
      .extracting(EmailMessage::getSubject, EmailMessage::getMessage)
      .containsExactly("Update your SonarQube Server's Quality Gate metrics",
        """
          We are sending this message because this version of SonarQube Server is in Standard Experience and some of your quality gates conditions are using metrics from Multi-Quality Rule (MQR) Mode.

          We recommend you update them to ensure accurate categorization and ranking of your issues.

          Go to the Quality Gates page, and we will guide you through the process.""");
  }

  @Test
  void format_whenMQRMode_shouldReturnExpectEmailMessage() {
    Assertions.assertThat(underTest.format(new QualityGateMetricsUpdateNotification(true)))
      .extracting(EmailMessage::getSubject, EmailMessage::getMessage)
      .containsExactly("Update your SonarQube Server's Quality Gate metrics",
        """
          We are sending this message because this version of SonarQube Server is in Multi-Quality Rule (MQR) Mode and some of your quality gates conditions are using metrics from Standard Experience.

          We recommend you update them to ensure accurate categorization and ranking of your issues.

          Go to the Quality Gates page, and we will guide you through the process.""");
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
