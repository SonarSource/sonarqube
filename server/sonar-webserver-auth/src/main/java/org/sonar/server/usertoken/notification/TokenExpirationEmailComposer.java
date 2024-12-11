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
package org.sonar.server.usertoken.notification;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.apache.commons.mail2.core.EmailException;
import org.apache.commons.mail2.jakarta.HtmlEmail;
import org.sonar.api.platform.Server;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.email.EmailSender;
import org.sonar.server.email.EmailSmtpConfiguration;
import org.sonar.server.oauth.OAuthMicrosoftRestClient;

import static java.lang.String.format;
import static org.sonar.db.user.TokenType.PROJECT_ANALYSIS_TOKEN;

public class TokenExpirationEmailComposer extends EmailSender<TokenExpirationEmail> {

  protected TokenExpirationEmailComposer(EmailSmtpConfiguration emailSmtpConfiguration, Server server, OAuthMicrosoftRestClient oAuthMicrosoftRestClient) {
    super(emailSmtpConfiguration, server, oAuthMicrosoftRestClient);
  }

  @Override protected void addReportContent(HtmlEmail email, TokenExpirationEmail emailData) throws EmailException {
    email.addTo(emailData.getRecipients().toArray(String[]::new));
    UserTokenDto token = emailData.getUserToken();
    if (token.isExpired()) {
      email.setSubject(format("Your token \"%s\" has expired.", token.getName()));
    } else {
      email.setSubject(format("Your token \"%s\" will expire.", token.getName()));
    }
    email.setHtmlMsg(composeEmailBody(token));
  }

  private String composeEmailBody(UserTokenDto token) {
    StringBuilder builder = new StringBuilder();
    if (token.isExpired()) {
      builder.append(format("Your token \"%s\" has expired.<br/><br/>", token.getName()));
    } else {
      builder.append(format("Your token \"%s\" will expire on %s.<br/><br/>", token.getName(), parseDate(token.getExpirationDate())));
    }
    builder
      .append("Token Summary<br/><br/>")
      .append(format("Name: %s<br/>", token.getName()))
      .append(format("Type: %s<br/>", token.getType()));
    if (PROJECT_ANALYSIS_TOKEN.name().equals(token.getType())) {
      builder.append(format("Project: %s<br/>", token.getProjectName()));
    }
    builder.append(format("Created on: %s<br/>", parseDate(token.getCreatedAt())));
    if (token.getLastConnectionDate() != null) {
      builder.append(format("Last used on: %s<br/>", parseDate(token.getLastConnectionDate())));
    }
    builder.append(format("%s on: %s<br/>", token.isExpired() ? "Expired" : "Expires", parseDate(token.getExpirationDate())))
      .append(
        format("<br/>If this token is still needed, please consider <a href=\"%s/account/security/\">generating</a> an equivalent.<br/><br/>", server.getPublicRootUrl()))
      .append("Don't forget to update the token in the locations where it is in use. "
        + "This may include the CI pipeline that analyzes your projects, "
        + "the IDE settings that connect SonarLint to SonarQube, "
        + "and any places where you make calls to web services.");
    return builder.toString();
  }

  private static String parseDate(long timestamp) {
    return Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
  }
}
