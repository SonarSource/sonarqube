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
package org.sonar.plugins.emailnotifications.templates.reviews;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.database.model.User;
import org.sonar.api.notifications.Notification;
import org.sonar.api.security.UserFinder;
import org.sonar.plugins.emailnotifications.api.EmailMessage;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReviewEmailTemplateTest {

  private ReviewEmailTemplate template;

  @Before
  public void setUp() {
    EmailSettings configuration = mock(EmailSettings.class);
    when(configuration.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
    UserFinder userFinder = mock(UserFinder.class);
    when(userFinder.findByLogin(eq("freddy.mallet"))).thenReturn(new User().setName("Freddy Mallet"));
    when(userFinder.findByLogin(eq("simon.brandhof"))).thenReturn(new User().setName("Simon Brandhof"));
    when(userFinder.findByLogin(eq("evgeny.mandrikov"))).thenReturn(new User().setName("Evgeny Mandrikov"));
    template = new ReviewEmailTemplate(configuration, userFinder);
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Freddy Mallet
   * 
   * Project: Sonar
   * Resource: org.sonar.server.ui.DefaultPages
   * 
   * Utility classes should not have a public or default constructor.
   * 
   * Comment:
   *   This is my first comment
   * 
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void should_format_comment_added() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("project", "Sonar")
        .setFieldValue("resource", "org.sonar.server.ui.DefaultPages")
        .setFieldValue("title", "Utility classes should not have a public or default constructor.")
        .setFieldValue("author", "freddy.mallet")
        .setFieldValue("old.comment", null)
        .setFieldValue("new.comment", "This is my first comment");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId()).isEqualTo("review/1");
    assertThat(message.getSubject()).isEqualTo("Review #1");
    assertThat(message.getFrom()).isEqualTo("Freddy Mallet");
    assertThat(message.getMessage()).isEqualTo("" +
        "Project: Sonar\n" +
        "Resource: org.sonar.server.ui.DefaultPages\n" +
        "\n" +
        "Utility classes should not have a public or default constructor.\n" +
        "\n" +
        "Comment:\n" +
        "  This is my first comment\n" +
        "\n" +
        "See it in Sonar: http://nemo.sonarsource.org/reviews/view/1\n");
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Freddy Mallet
   * 
   * Project: Sonar
   * Resource: org.sonar.server.ui.DefaultPages
   * 
   * Utility classes should not have a public or default constructor.
   * 
   * Comment:
   *   This is another comment
   * Was:
   *   This is my first comment
   * 
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void should_format_commentedited() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("project", "Sonar")
        .setFieldValue("resource", "org.sonar.server.ui.DefaultPages")
        .setFieldValue("title", "Utility classes should not have a public or default constructor.")
        .setFieldValue("author", "freddy.mallet")
        .setFieldValue("old.comment", "This is my first comment")
        .setFieldValue("new.comment", "This is another comment");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId()).isEqualTo("review/1");
    assertThat(message.getSubject()).isEqualTo("Review #1");
    assertThat(message.getFrom()).isEqualTo("Freddy Mallet");
    assertThat(message.getMessage()).isEqualTo("" +
        "Project: Sonar\n" +
        "Resource: org.sonar.server.ui.DefaultPages\n" +
        "\n" +
        "Utility classes should not have a public or default constructor.\n" +
        "\n" +
        "Comment:\n" +
        "  This is another comment\n" +
        "Was:\n" +
        "  This is my first comment\n" +
        "\n" +
        "See it in Sonar: http://nemo.sonarsource.org/reviews/view/1\n");
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Freddy Mallet
   * 
   * Project: Sonar
   * Resource: org.sonar.server.ui.DefaultPages
   * 
   * Utility classes should not have a public or default constructor.
   * 
   * Comment deleted, was:
   *   This is deleted comment
   * 
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void should_format_comment_deleted() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("project", "Sonar")
        .setFieldValue("resource", "org.sonar.server.ui.DefaultPages")
        .setFieldValue("title", "Utility classes should not have a public or default constructor.")
        .setFieldValue("old.comment", "This is deleted comment")
        .setFieldValue("new.comment", null)
        .setFieldValue("author", "freddy.mallet");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId()).isEqualTo("review/1");
    assertThat(message.getSubject()).isEqualTo("Review #1");
    assertThat(message.getFrom()).isEqualTo("Freddy Mallet");
    assertThat(message.getMessage()).isEqualTo("" +
        "Project: Sonar\n" +
        "Resource: org.sonar.server.ui.DefaultPages\n" +
        "\n" +
        "Utility classes should not have a public or default constructor.\n" +
        "\n" +
        "Comment deleted, was:\n" +
        "  This is deleted comment\n" +
        "\n" +
        "See it in Sonar: http://nemo.sonarsource.org/reviews/view/1\n");
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Freddy Mallet
   * 
   * Project: Sonar
   * Resource: org.sonar.server.ui.DefaultPages
   * 
   * Utility classes should not have a public or default constructor.
   * 
   * Assignee: Evgeny Mandrikov
   * 
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void should_format_assigneed() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("project", "Sonar")
        .setFieldValue("resource", "org.sonar.server.ui.DefaultPages")
        .setFieldValue("title", "Utility classes should not have a public or default constructor.")
        .setFieldValue("author", "freddy.mallet")
        .setFieldValue("old.assignee", null)
        .setFieldValue("new.assignee", "evgeny.mandrikov");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId()).isEqualTo("review/1");
    assertThat(message.getSubject()).isEqualTo("Review #1");
    assertThat(message.getFrom()).isEqualTo("Freddy Mallet");
    assertThat(message.getMessage()).isEqualTo("" +
        "Project: Sonar\n" +
        "Resource: org.sonar.server.ui.DefaultPages\n" +
        "\n" +
        "Utility classes should not have a public or default constructor.\n" +
        "\n" +
        "Assignee: Evgeny Mandrikov\n" +
        "\n" +
        "See it in Sonar: http://nemo.sonarsource.org/reviews/view/1\n");
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Freddy Mallet
   * 
   * Project: Sonar
   * Resource: org.sonar.server.ui.DefaultPages
   * 
   * Utility classes should not have a public or default constructor.
   * 
   * Assignee: Simon Brandhof (was Evgeny Mandrikov)
   * 
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void should_format_assigneed_to_another_person() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("project", "Sonar")
        .setFieldValue("resource", "org.sonar.server.ui.DefaultPages")
        .setFieldValue("title", "Utility classes should not have a public or default constructor.")
        .setFieldValue("author", "freddy.mallet")
        .setFieldValue("old.assignee", "evgeny.mandrikov")
        .setFieldValue("new.assignee", "simon.brandhof");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId()).isEqualTo("review/1");
    assertThat(message.getSubject()).isEqualTo("Review #1");
    assertThat(message.getFrom()).isEqualTo("Freddy Mallet");
    assertThat(message.getMessage()).isEqualTo("" +
        "Project: Sonar\n" +
        "Resource: org.sonar.server.ui.DefaultPages\n" +
        "\n" +
        "Utility classes should not have a public or default constructor.\n" +
        "\n" +
        "Assignee: Simon Brandhof (was Evgeny Mandrikov)\n" +
        "\n" +
        "See it in Sonar: http://nemo.sonarsource.org/reviews/view/1\n");
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Freddy Mallet
   * 
   * Project: Sonar
   * Resource: org.sonar.server.ui.DefaultPages
   * 
   * Utility classes should not have a public or default constructor.
   * 
   * Assignee: (was Simon Brandhof)
   * 
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void should_format_unassigned() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("project", "Sonar")
        .setFieldValue("resource", "org.sonar.server.ui.DefaultPages")
        .setFieldValue("title", "Utility classes should not have a public or default constructor.")
        .setFieldValue("author", "freddy.mallet")
        .setFieldValue("old.assignee", "simon.brandhof")
        .setFieldValue("new.assignee", null);
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId()).isEqualTo("review/1");
    assertThat(message.getSubject()).isEqualTo("Review #1");
    assertThat(message.getFrom()).isEqualTo("Freddy Mallet");
    assertThat(message.getMessage()).isEqualTo("" +
        "Project: Sonar\n" +
        "Resource: org.sonar.server.ui.DefaultPages\n" +
        "\n" +
        "Utility classes should not have a public or default constructor.\n" +
        "\n" +
        "Assignee:  (was Simon Brandhof)\n" +
        "\n" +
        "See it in Sonar: http://nemo.sonarsource.org/reviews/view/1\n");
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Sonar
   * 
   * Project: Sonar
   * Resource: org.sonar.server.ui.DefaultPages
   * 
   * Utility classes should not have a public or default constructor.
   * 
   * Status: CLOSED (was OPEN)
   * 
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void should_format_closed() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("project", "Sonar")
        .setFieldValue("resource", "org.sonar.server.ui.DefaultPages")
        .setFieldValue("title", "Utility classes should not have a public or default constructor.")
        .setFieldValue("old.status", "OPEN")
        .setFieldValue("new.status", "CLOSED");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId()).isEqualTo("review/1");
    assertThat(message.getSubject()).isEqualTo("Review #1");
    assertThat(message.getFrom()).isNull();
    assertThat(message.getMessage()).isEqualTo("" +
        "Project: Sonar\n" +
        "Resource: org.sonar.server.ui.DefaultPages\n" +
        "\n" +
        "Utility classes should not have a public or default constructor.\n" +
        "\n" +
        "Status: CLOSED (was OPEN)\n" +
        "\n" +
        "See it in Sonar: http://nemo.sonarsource.org/reviews/view/1\n");
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Simon Brandhof
   * 
   * Project: Sonar
   * Resource: org.sonar.server.ui.DefaultPages
   * 
   * Utility classes should not have a public or default constructor.
   * 
   * Status: REOPENED (was RESOLVED)
   * Resolution: (was FIXED)
   * 
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void should_format_reopened() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("project", "Sonar")
        .setFieldValue("resource", "org.sonar.server.ui.DefaultPages")
        .setFieldValue("title", "Utility classes should not have a public or default constructor.")
        .setFieldValue("old.resolution", "FIXED")
        .setFieldValue("new.resolution", null)
        .setFieldValue("old.status", "RESOLVED")
        .setFieldValue("new.status", "REOPENED");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId()).isEqualTo("review/1");
    assertThat(message.getSubject()).isEqualTo("Review #1");
    assertThat(message.getFrom()).isNull();
    assertThat(message.getMessage()).isEqualTo("" +
        "Project: Sonar\n" +
        "Resource: org.sonar.server.ui.DefaultPages\n" +
        "\n" +
        "Utility classes should not have a public or default constructor.\n" +
        "\n" +
        "Status: REOPENED (was RESOLVED)\n" +
        "Resolution:  (was FIXED)\n" +
        "\n" +
        "See it in Sonar: http://nemo.sonarsource.org/reviews/view/1\n");
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Simon Brandhof
   * 
   * Project: Sonar
   * Resource: org.sonar.server.ui.DefaultPages
   * 
   * Utility classes should not have a public or default constructor.
   * 
   * Status: RESOLVED (was OPEN)
   * Resolution: FIXED
   * 
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void should_format_resolved_as_fixed() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("project", "Sonar")
        .setFieldValue("resource", "org.sonar.server.ui.DefaultPages")
        .setFieldValue("title", "Utility classes should not have a public or default constructor.")
        .setFieldValue("author", "simon.brandhof")
        .setFieldValue("old.status", "OPEN")
        .setFieldValue("old.resolution", null)
        .setFieldValue("new.status", "RESOLVED")
        .setFieldValue("new.resolution", "FIXED");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId()).isEqualTo("review/1");
    assertThat(message.getSubject()).isEqualTo("Review #1");
    assertThat(message.getFrom()).isEqualTo("Simon Brandhof");
    assertThat(message.getMessage()).isEqualTo("" +
        "Project: Sonar\n" +
        "Resource: org.sonar.server.ui.DefaultPages\n" +
        "\n" +
        "Utility classes should not have a public or default constructor.\n" +
        "\n" +
        "Status: RESOLVED (was OPEN)\n" +
        "Resolution: FIXED\n" +
        "\n" +
        "See it in Sonar: http://nemo.sonarsource.org/reviews/view/1\n");
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Simon Brandhof
   * 
   * Project: Sonar
   * Resource: org.sonar.server.ui.DefaultPages
   * 
   * Utility classes should not have a public or default constructor.
   * 
   * Status: RESOLVED (was REOPENED)
   * Resolution: FALSE-POSITIVE
   * Comment:
   *   Because!
   * 
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void should_format_resolved_as_false_positive() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("project", "Sonar")
        .setFieldValue("resource", "org.sonar.server.ui.DefaultPages")
        .setFieldValue("title", "Utility classes should not have a public or default constructor.")
        .setFieldValue("author", "freddy.mallet")
        .setFieldValue("old.status", "REOPENED")
        .setFieldValue("old.resolution", null)
        .setFieldValue("new.status", "RESOLVED")
        .setFieldValue("new.resolution", "FALSE-POSITIVE")
        .setFieldValue("new.comment", "Because!");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId()).isEqualTo("review/1");
    assertThat(message.getSubject()).isEqualTo("Review #1 - False Positive");
    assertThat(message.getFrom()).isEqualTo("Freddy Mallet");
    assertThat(message.getMessage()).isEqualTo("" +
        "Project: Sonar\n" +
        "Resource: org.sonar.server.ui.DefaultPages\n" +
        "\n" +
        "Utility classes should not have a public or default constructor.\n" +
        "\n" +
        "Status: RESOLVED (was REOPENED)\n" +
        "Resolution: FALSE-POSITIVE\n" +
        "Comment:\n" +
        "  Because!\n" +
        "\n" +
        "See it in Sonar: http://nemo.sonarsource.org/reviews/view/1\n");
  }

  @Test
  public void should_not_format() {
    Notification notification = new Notification("other");
    EmailMessage message = template.format(notification);
    assertThat(message).isNull();
  }

  @Test
  public void should_return_full_name_or_login() {
    assertThat(template.getUserFullName("freddy.mallet")).isEqualTo("Freddy Mallet");
    assertThat(template.getUserFullName("deleted")).isEqualTo("deleted");
    assertThat(template.getUserFullName(null)).isNull();
  }

}
