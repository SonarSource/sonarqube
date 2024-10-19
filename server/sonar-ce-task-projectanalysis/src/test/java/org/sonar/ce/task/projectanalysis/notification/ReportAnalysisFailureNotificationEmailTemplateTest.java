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
package org.sonar.ce.task.projectanalysis.notification;

import org.junit.Test;
import org.sonar.api.config.EmailSettings;
import org.sonar.server.qualitygate.notification.QGChangeNotification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportAnalysisFailureNotificationEmailTemplateTest {
  ReportAnalysisFailureNotificationSerializer serializer = new ReportAnalysisFailureNotificationSerializerImpl();
  EmailSettings settings = mock(EmailSettings.class);

  ReportAnalysisFailureNotificationEmailTemplate underTest = new ReportAnalysisFailureNotificationEmailTemplate(serializer, settings);

  @Test
  public void should_not_format_other_than_analysis_failure() {
    assertThat(underTest.format(new QGChangeNotification())).isNull();
  }

  @Test
  public void check_formatting() {
    ReportAnalysisFailureNotification notification = mock(ReportAnalysisFailureNotification.class);

    when(notification.getFieldValue("project.uuid")).thenReturn("uuid");
    when(notification.getFieldValue("project.name")).thenReturn("name");
    when(notification.getFieldValue("project.key")).thenReturn("key");
    when(notification.getFieldValue("project.branch")).thenReturn("branch");
    when(notification.getFieldValue("task.uuid")).thenReturn("task_uuid");
    when(notification.getFieldValue("task.createdAt")).thenReturn("1673449576159");
    when(notification.getFieldValue("task.failedAt")).thenReturn("1673449576159");
    when(notification.getFieldValue("error.message")).thenReturn("error");

    when(settings.getServerBaseURL()).thenReturn("sonarsource.com");

    var result = underTest.format(notification);

    assertThat(result.getSubject()).isEqualTo("name: Background task in failure");
    assertThat(result.getMessage())
      .contains("""
        Project:	name
        Background task:	task_uuid""")
      .contains("""
        Error message:	error
              
        More details at: sonarsource.com/project/background_tasks?id=key""");
  }
}
