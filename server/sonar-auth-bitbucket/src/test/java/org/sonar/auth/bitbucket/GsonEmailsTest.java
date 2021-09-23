/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.auth.bitbucket;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GsonEmailsTest {

  @Test
  public void testParse() {
    String json = "{" +
      "\"pagelen\": 10," +
      "\"values\": [" +
      "{" +
      "\"is_primary\": true," +
      "\"is_confirmed\": true," +
      "\"type\": \"email\"," +
      "\"email\": \"foo@bar.com\"," +
      "\"links\": {" +
      "\"self\": {" +
      "\"href\": \"https://api.bitbucket.org/2.0/user/emails/foo@bar.com\"" +
      "}" +
      "}" +
      "}" +
      "]," +
      "\"page\": 1," +
      "\"size\": 1" +
      "}";
    GsonEmails emails = GsonEmails.parse(json);
    assertThat(emails.getEmails()).hasSize(1);
    assertThat(emails.getEmails().get(0).isPrimary()).isTrue();
    assertThat(emails.getEmails().get(0).getEmail()).isEqualTo("foo@bar.com");
  }

  @Test
  public void test_extractPrimaryEmail() {
    String json = "{" +
      "\"pagelen\": 10," +
      "\"values\": [" +
      "{" +
      "\"is_primary\": false," +
      "\"is_confirmed\": true," +
      "\"type\": \"email\"," +
      "\"email\": \"secondary@bar.com\"," +
      "\"links\": {" +
      "\"self\": {" +
      "\"href\": \"https://api.bitbucket.org/2.0/user/emails/secondary@bar.com\"" +
      "}" +
      "}" +
      "}," +
      "{" +
      "\"is_primary\": true," +
      "\"is_confirmed\": true," +
      "\"type\": \"email\"," +
      "\"email\": \"primary@bar.com\"," +
      "\"links\": {" +
      "\"self\": {" +
      "\"href\": \"https://api.bitbucket.org/2.0/user/emails/primary@bar.com\"" +
      "}" +
      "}" +
      "}" +
      "]," +
      "\"page\": 1," +
      "\"size\": 2" +
      "}";
    String email = GsonEmails.parse(json).extractPrimaryEmail();
    assertThat(email).isEqualTo("primary@bar.com");
  }

  @Test
  public void test_extractPrimaryEmail_not_found() {
    String json = "{" +
      "\"pagelen\": 10," +
      "\"values\": [" +
      "]," +
      "\"page\": 1," +
      "\"size\": 0" +
      "}";
    String email = GsonEmails.parse(json).extractPrimaryEmail();
    assertThat(email).isNull();
  }
}
