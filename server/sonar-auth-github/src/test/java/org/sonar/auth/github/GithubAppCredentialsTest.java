/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.auth.github;

import com.google.gson.Gson;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GithubAppCredentialsTest {

  private static final Gson GSON = new Gson();

  @Test
  public void getAppId_returnsIdAsString() {
    GithubAppCredentials underTest = new GithubAppCredentials(123456L, "my-app", "client-id", "client-secret", "webhook-secret", "pem", "https://github.com/apps/my-app");

    assertThat(underTest.getAppId()).isEqualTo("123456");
  }

  @Test
  public void deserialize_mapsAllFieldsFromJson() {
    GithubAppCredentials underTest = GSON.fromJson("""
      {
        "id": 123456,
        "slug": "my-app",
        "client_id": "Iv1.client",
        "client_secret": "the-secret",
        "webhook_secret": "the-webhook-secret",
        "pem": "-----BEGIN RSA PRIVATE KEY-----",
        "html_url": "https://github.com/apps/my-app"
      }""", GithubAppCredentials.class);

    assertThat(underTest.id()).isEqualTo(123456L);
    assertThat(underTest.getAppId()).isEqualTo("123456");
    assertThat(underTest.slug()).isEqualTo("my-app");
    assertThat(underTest.clientId()).isEqualTo("Iv1.client");
    assertThat(underTest.clientSecret()).isEqualTo("the-secret");
    assertThat(underTest.webhookSecret()).isEqualTo("the-webhook-secret");
    assertThat(underTest.pem()).isEqualTo("-----BEGIN RSA PRIVATE KEY-----");
    assertThat(underTest.htmlUrl()).isEqualTo("https://github.com/apps/my-app");
  }

  @Test
  public void deserialize_nullableFieldsCanBeAbsent() {
    GithubAppCredentials underTest = GSON.fromJson("""
      {
        "id": 7,
        "client_id": "Iv1.client",
        "client_secret": "the-secret",
        "pem": "-----BEGIN RSA PRIVATE KEY-----"
      }""", GithubAppCredentials.class);

    assertThat(underTest.getAppId()).isEqualTo("7");
    assertThat(underTest.clientId()).isEqualTo("Iv1.client");
    assertThat(underTest.clientSecret()).isEqualTo("the-secret");
    assertThat(underTest.pem()).isEqualTo("-----BEGIN RSA PRIVATE KEY-----");
    assertThat(underTest.slug()).isNull();
    assertThat(underTest.webhookSecret()).isNull();
    assertThat(underTest.htmlUrl()).isNull();
  }
}
