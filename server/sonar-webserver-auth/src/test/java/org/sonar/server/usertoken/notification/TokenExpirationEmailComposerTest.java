/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import org.apache.commons.mail2.core.EmailException;
import org.apache.commons.mail2.jakarta.HtmlEmail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.platform.Server;
import org.sonar.db.user.TokenType;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.email.EmailSmtpConfiguration;
import org.sonar.server.oauth.OAuthMicrosoftRestClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenExpirationEmailComposerTest {
  private final EmailSmtpConfiguration emailSmtpConfiguration = mock();
  private final Server server = mock();
  private final long createdAt = LocalDate.parse("2022-01-01").atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
  private final TokenExpirationEmailComposer underTest = new TokenExpirationEmailComposer(emailSmtpConfiguration, server, mock(OAuthMicrosoftRestClient.class));

  @BeforeEach
  void setup() {
    when(server.getPublicRootUrl()).thenReturn("http://localhost");
  }

  @Test
  void composer_email_with_expiring_project_token() throws EmailException {
    long expiredDate = LocalDate.now().atStartOfDay(ZoneOffset.UTC).plusDays(7).toInstant().toEpochMilli();
    var token = createToken("projectToken", "projectA", expiredDate);
    var emailData = new TokenExpirationEmail("admin@sonarsource.com", token);
    var email = mock(HtmlEmail.class);
    underTest.addReportContent(email, emailData);
    verify(email).setSubject("Your token \"projectToken\" will expire.");
    verify(email).setHtmlMsg(
      String.format("Your token \"projectToken\" will expire on %s.<br/><br/>"
          + "Token Summary<br/><br/>"
          + "Name: projectToken<br/>"
          + "Type: PROJECT_ANALYSIS_TOKEN<br/>"
          + "Project: projectA<br/>"
          + "Created on: January 01, 2022<br/>"
          + "Last used on: January 01, 2022<br/>"
          + "Expires on: %s<br/><br/>"
          + "If this token is still needed, please consider <a href=\"http://localhost/account/security/\">generating</a> an equivalent.<br/><br/>"
          + "Don't forget to update the token in the locations where it is in use. This may include the CI pipeline that analyzes your projects, the IDE settings that connect SonarQube IDE to SonarQube Server, and any places where you make calls to web services.",
        parseDate(expiredDate), parseDate(expiredDate)));
  }

  @Test
  void composer_email_with_expired_global_token() throws EmailException {
    long expiredDate = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    var token = createToken("globalToken", null, expiredDate);
    var emailData = new TokenExpirationEmail("admin@sonarsource.com", token);
    var email = mock(HtmlEmail.class);
    underTest.addReportContent(email, emailData);
    verify(email).setSubject("Your token \"globalToken\" has expired.");
    verify(email).setHtmlMsg(
      String.format("Your token \"globalToken\" has expired.<br/><br/>"
          + "Token Summary<br/><br/>"
          + "Name: globalToken<br/>"
          + "Type: GLOBAL_ANALYSIS_TOKEN<br/>"
          + "Created on: January 01, 2022<br/>"
          + "Last used on: January 01, 2022<br/>"
          + "Expired on: %s<br/><br/>"
          + "If this token is still needed, please consider <a href=\"http://localhost/account/security/\">generating</a> an equivalent.<br/><br/>"
          + "Don't forget to update the token in the locations where it is in use. This may include the CI pipeline that analyzes your projects, the IDE settings that connect SonarQube IDE to SonarQube Server, and any places where you make calls to web services.",
        parseDate(expiredDate)));
  }


  @ParameterizedTest
  @MethodSource("tokenExpirationAndExpectedEmailSubject")
  void addReportContent_givenConfigurationHasPrefixAndIsAboutToExpire_addPrefixToEmailSubject(long expiredDate, String expectedSubject) throws EmailException {
    var token = createToken("projectToken", "projectA", expiredDate);
    var emailData = new TokenExpirationEmail("admin@sonarsource.com", token);
    HtmlEmail email = mock();
    when(emailSmtpConfiguration.getPrefix()).thenReturn("[PREFIX]");
    underTest.addReportContent(email, emailData);
    verify(email).setSubject(expectedSubject);
  }


  private static Stream<Arguments> tokenExpirationAndExpectedEmailSubject() {
    return Stream.of(
      Arguments.of(LocalDate.now().atStartOfDay(ZoneOffset.UTC).plusDays(7).toInstant().toEpochMilli(), "[PREFIX] Your token \"projectToken\" will expire."),
      Arguments.of(LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), "[PREFIX] Your token \"projectToken\" has expired.")
    );
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
    return Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
  }
}
