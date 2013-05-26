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
package org.sonar.plugins.core.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.plugins.emailnotifications.api.EmailMessage;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
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
    assertThat(message, nullValue());
  }

  /**
   * <pre>
   * Subject: New issues for project Foo
   * From: Sonar
   *
   * Project: Foo
   * 32 new issues
   *
   * See it in Sonar: http://nemo.sonarsource.org/drilldown/measures/org.sonar.foo:foo?metric=new_violations
   * </pre>
   */
  @Test
  public void shouldFormatCommentAdded() {
    Notification notification = new Notification("new-issues")
      .setFieldValue("count", "32")
      .setFieldValue("projectName", "Foo")
      .setFieldValue("projectKey", "org.sonar.foo:foo")
      .setFieldValue("projectId", "45");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("new-issues/45"));
    assertThat(message.getSubject(), is("New issues for project Foo"));
    assertThat(message.getMessage(), is("" +
      "Project: Foo\n" +
      "32 new issues\n" +
      "\n" +
      "See it in Sonar: http://nemo.sonarsource.org/drilldown/measures/org.sonar.foo:foo?metric=new_violations\n"));
  }

}
