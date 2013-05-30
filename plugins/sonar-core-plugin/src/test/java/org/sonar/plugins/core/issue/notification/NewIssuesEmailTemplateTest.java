/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.core.issue.notification;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.plugins.emailnotifications.api.EmailMessage;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NewIssuesEmailTemplateTest {

  NewIssuesEmailTemplate template;

  @Before
  public void setUp() {
    EmailSettings settings = mock(EmailSettings.class);
    when(settings.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
    template = new NewIssuesEmailTemplate(settings);
  }

  @Test
  public void shouldNotFormatIfNotCorrectNotification() {
    Notification notification = new Notification("other-notif");
    EmailMessage message = template.format(notification);
    assertThat(message).isNull();
  }

  /**
   * <pre>
   * Subject: Project Struts, new issues
   * From: Sonar
   *
   * Project: Foo
   * 32 new issues
   *
   * See it in SonarQube: http://nemo.sonarsource.org/drilldown/measures/org.sonar.foo:foo?metric=new_violations
   * </pre>
   */
  @Test
  public void shouldFormatCommentAdded() {
    Notification notification = new Notification("new-issues")
      .setFieldValue("count", "32")
      .setFieldValue("projectName", "Struts")
      .setFieldValue("projectKey", "org.apache:struts")
      .setFieldValue("projectDate", "2010-05-18T15:50:45+0100");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId()).isEqualTo("new-issues/org.apache:struts");
    assertThat(message.getSubject()).isEqualTo("Project Struts, new issues");
    assertThat(message.getMessage()).isEqualTo("" +
      "Project: Struts\n" +
      "32 new issues\n" +
      "\n" +
      "See it in SonarQube: http://nemo.sonarsource.org/issues/search?componentRoots=org.apache%3Astruts&createdAfter=2010-05-18\n");
  }

}
