/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.net.MalformedURLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.EmailSettings;
import org.sonar.db.user.TokenType;
import org.sonar.db.user.UserTokenDto;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TokenExpirationEmailComposerTest {
  private final EmailSettings emailSettings = mock(EmailSettings.class);
  private final long createdAt = LocalDate.parse("2022-01-01").atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
  private final TokenExpirationEmailComposer underTest = new TokenExpirationEmailComposer(emailSettings);

  @Before
  public void setup() {
    when(emailSettings.getServerBaseURL()).thenReturn("http://localhost");
  }

  @Test
  public void composer_email_with_expiring_project_token() throws MalformedURLException, EmailException {
    long expiredDate = LocalDate.now().atStartOfDay(ZoneOffset.UTC).plusDays(7).toInstant().toEpochMilli();
    var token = createToken("projectToken", "projectA", expiredDate);
    var emailData = new TokenExpirationEmail("admin@sonarsource.com", token);
    var email = mock(HtmlEmail.class);
    underTest.addReportContent(email, emailData);
    verify(email).setSubject(String.format("Your token with name \"projectToken\" will expire on %s.", parseDate(expiredDate)));
    verify(email).setHtmlMsg(String.format("Token Summary<br/><br/>"
        + "Name: projectToken<br/>"
        + "Type: PROJECT_ANALYSIS_TOKEN<br/>"
        + "Project: projectA<br/>"
        + "Created on: 01/01/2022<br/>"
        + "Last used on: 01/01/2022<br/>"
        + "Expires on: %s<br/><br/>"
        + "If this token is still needed, visit <a href=\"http://localhost/account/security/\">here</a> to generate an equivalent.",
      parseDate(expiredDate)));
  }

  @Test
  public void composer_email_with_expired_global_token() throws MalformedURLException, EmailException {
    long expiredDate = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    var token = createToken("globalToken", null, expiredDate);
    var emailData = new TokenExpirationEmail("admin@sonarsource.com", token);
    var email = mock(HtmlEmail.class);
    underTest.addReportContent(email, emailData);
    verify(email).setSubject("Your token with name \"globalToken\" has expired.");
    verify(email).setHtmlMsg(String.format("Token Summary<br/><br/>"
        + "Name: globalToken<br/>"
        + "Type: GLOBAL_ANALYSIS_TOKEN<br/>"
        + "Created on: 01/01/2022<br/>"
        + "Last used on: 01/01/2022<br/>"
        + "Expired on: %s<br/><br/>"
        + "If this token is still needed, visit <a href=\"http://localhost/account/security/\">here</a> to generate an equivalent.",
      parseDate(expiredDate)));
  }

  private UserTokenDto createToken(String name, String project, long expired) {
    var token = new UserTokenDto();
    token.setName(name);
    if (project != null) {
      token.setType(TokenType.PROJECT_ANALYSIS_TOKEN.name());
      token.setProjectName(project);
    } else {
      token.setType(TokenType.GLOBAL_ANALYSIS_TOKEN.name());
    }
    token.setCreatedAt(createdAt);
    token.setLastConnectionDate(createdAt);
    token.setExpirationDate(expired);
    return token;
  }

  private String parseDate(long timestamp) {
    return Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
  }
}
