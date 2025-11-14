/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.audit.model;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookNewValueTest {

  @Test
  void sanitize_url_replace_url() {
    var webhookNewValue = new WebhookNewValue("uuid", "name", "projectUuid", "projectKey", "projectName", "http://admin:admin@localhost" +
      ".com");
    webhookNewValue.sanitizeUrl(s -> s.replace("admin", "*****"));
    assertThat(webhookNewValue).hasToString("{"
      + "\"webhookUuid\": \"uuid\","
      + " \"name\": \"name\","
      + " \"url\": \"http://*****:*****@localhost.com\","
      + " \"projectUuid\": \"projectUuid\","
      + " \"projectKey\": \"projectKey\","
      + " \"projectName\": \"projectName\" }");
  }

  @Test
  void sanitize_url_do_nothing_when_url_is_null() {
    var webhookNewValue = new WebhookNewValue("uuid", "name", "projectUuid", "projectKey", "projectName", null);
    webhookNewValue.sanitizeUrl(s -> s.replace("admin", "*****"));
    assertThat(webhookNewValue).hasToString("{"
      + "\"webhookUuid\": \"uuid\","
      + " \"name\": \"name\","
      + " \"projectUuid\": \"projectUuid\","
      + " \"projectKey\": \"projectKey\","
      + " \"projectName\": \"projectName\" }");
  }

}
