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
package org.sonar.server.qualityprofile;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.Date;
import javax.annotation.CheckForNull;
import org.sonar.api.notifications.Notification;
import org.sonar.api.platform.Server;
import org.sonar.server.issue.notification.EmailMessage;
import org.sonar.server.issue.notification.EmailTemplate;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.api.utils.DateUtils.formatDate;
import static org.sonar.server.qualityprofile.BuiltInQPChangeNotificationBuilder.Profile;
import static org.sonar.server.qualityprofile.BuiltInQPChangeNotificationBuilder.parse;

public class BuiltInQPChangeNotificationTemplate implements EmailTemplate {

  private final Server server;

  public BuiltInQPChangeNotificationTemplate(Server server) {
    this.server = server;
  }

  @Override
  @CheckForNull
  public EmailMessage format(Notification notification) {
    if (!BuiltInQPChangeNotification.TYPE.equals(notification.getType())) {
      return null;
    }

    BuiltInQPChangeNotificationBuilder profilesNotification = parse(notification);
    StringBuilder message = new StringBuilder("The following built-in profiles have been updated:\n\n");
    profilesNotification.getProfiles().stream()
      .sorted(Comparator.comparing(Profile::getLanguageName).thenComparing(Profile::getProfileName))
      .forEach(profile -> {
        message.append("\"")
          .append(profile.getProfileName())
          .append("\" - ")
          .append(profile.getLanguageName())
          .append(": ")
          .append(server.getPublicRootUrl()).append("/profiles/changelog?language=")
          .append(profile.getLanguageKey())
          .append("&name=")
          .append(encode(profile.getProfileName()))
          .append("&since=")
          .append(formatDate(new Date(profile.getStartDate())))
          .append("&to=")
          .append(formatDate(new Date(profile.getEndDate())))
          .append("\n");
        int newRules = profile.getNewRules();
        if (newRules > 0) {
          message.append(" ").append(newRules).append(" new rule")
            .append(plural(newRules))
            .append('\n');
        }
        int updatedRules = profile.getUpdatedRules();
        if (updatedRules > 0) {
          message.append(" ").append(updatedRules).append(" rule")
            .append(updatedRules > 1 ? "s have been updated" : " has been updated")
            .append("\n");
        }
        int removedRules = profile.getRemovedRules();
        if (removedRules > 0) {
          message.append(" ").append(removedRules).append(" rule")
            .append(plural(removedRules))
            .append(" removed\n");
        }
        message.append("\n");
      });

    message.append("This is a good time to review your quality profiles and update them to benefit from the latest evolutions: ");
    message.append(server.getPublicRootUrl()).append("/profiles");

    // And finally return the email that will be sent
    return new EmailMessage()
      .setMessageId(BuiltInQPChangeNotification.TYPE)
      .setSubject("Built-in quality profiles have been updated")
      .setPlainTextMessage(message.toString());
  }

  private static String plural(int count) {
    return count > 1 ? "s" : "";
  }

  public String encode(String text) {
    try {
      return URLEncoder.encode(text, UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(String.format("Cannot encode %s", text), e);
    }
  }
}
