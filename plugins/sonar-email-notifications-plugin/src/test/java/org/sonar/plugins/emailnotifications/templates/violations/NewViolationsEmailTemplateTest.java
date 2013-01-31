/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.emailnotifications.templates.violations;

import org.sonar.plugins.emailnotifications.templates.violations.NewViolationsEmailTemplate;

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

public class NewViolationsEmailTemplateTest {

  private NewViolationsEmailTemplate template;

  @Before
  public void setUp() {
    EmailSettings configuration = mock(EmailSettings.class);
    when(configuration.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
    template = new NewViolationsEmailTemplate(configuration);
  }

  @Test
  public void shouldNotFormatIfNotCorrectNotification() {
    Notification notification = new Notification("other-notif");
    EmailMessage message = template.format(notification);
    assertThat(message, nullValue());
  }

  /**
   * <pre>
   * Subject: New violations for project Foo
   * From: Sonar
   * 
   * Project: Foo
   * 32 new violations introduced since 2012-01-02
   * 
   * See it in Sonar: http://nemo.sonarsource.org/drilldown/measures/org.sonar.foo:foo?metric=new_violations&period=1
   * </pre>
   */
  @Test
  public void shouldFormatCommentAdded() {
    Notification notification = new Notification("new-violations")
        .setFieldValue("count", "32")
        .setFieldValue("projectName", "Foo")
        .setFieldValue("projectKey", "org.sonar.foo:foo")
        .setFieldValue("projectId", "45")
        .setFieldValue("fromDate", "2012-01-02");

    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("new-violations/45"));
    assertThat(message.getSubject(), is("New violations for project Foo"));
    assertThat(message.getMessage(), is("" +
      "Project: Foo\n" +
      "32 new violations introduced since 2012-01-02\n" +
      "\n" +
      "See it in Sonar: http://nemo.sonarsource.org/drilldown/measures/org.sonar.foo:foo?metric=new_violations&period=1\n"));
  }

}
