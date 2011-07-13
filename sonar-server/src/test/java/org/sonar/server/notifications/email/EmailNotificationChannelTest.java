/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.server.notifications.email;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class EmailNotificationChannelTest {

  private EmailNotificationChannel channel;

  @Before
  public void setUp() {
    channel = new EmailNotificationChannel(null);
  }

  @Test
  public void shouldCreateEmail() {
    EmailMessage email = new EmailMessage()
        .setMessageId("reviews/view/1")
        .setFrom("Evgeny Mandrikov")
        .setTo("simon.brandhof@sonarcource.com")
        .setSubject("Review #3")
        .setMessage("I'll take care of this violation.");
    String expected = "" +
        "Message-Id: <reviews/view/1@nemo.sonarsource.org>\n" +
        "In-Reply-To: <reviews/view/1@nemo.sonarsource.org>\n" +
        "References: <reviews/view/1@nemo.sonarsource.org>\n" +
        "List-Id: <sonar.nemo.sonarsource.org>\n" +
        "List-Archive: http://nemo.sonarsource.org\n" +
        "From: Evgeny Mandrikov <noreply@nemo.sonarsource.org>\n" +
        "To: simon.brandhof@sonarcource.com\n" +
        "Subject: Re: [Sonar] Review #3\n" +
        "\n" +
        "I'll take care of this violation.\n" +
        "\n" +
        "--\n" +
        "View it in Sonar: http://nemo.sonarsource.org/reviews/view/1";
    String message = channel.create(email);
    System.out.println(message);
    assertThat(message, is(expected));
  }

  @Test
  public void shouldCreateDefaultEmail() {
    EmailMessage email = new EmailMessage()
        .setTo("simon.brandhof@sonarcource.com")
        .setMessage("Message");
    String expected = "" +
        "List-Id: <sonar.nemo.sonarsource.org>\n" +
        "List-Archive: http://nemo.sonarsource.org\n" +
        "From: Sonar <noreply@nemo.sonarsource.org>\n" +
        "To: simon.brandhof@sonarcource.com\n" +
        "Subject: [Sonar] Notification\n" +
        "\n" +
        "Message\n";
    String message = channel.create(email);
    System.out.println(message);
    assertThat(message, is(expected));
  }

}
