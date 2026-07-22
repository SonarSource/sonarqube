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
package org.sonar.server.almsettings.ws;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import org.sonar.api.platform.Server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GithubAppManifestGeneratorTest {

  private final Server server = mock(Server.class);
  private final GithubAppManifestGenerator underTest = new GithubAppManifestGenerator(server);

  @Test
  public void generateManifest_buildsExpectedManifest() {
    when(server.getPublicRootUrl()).thenReturn("https://sonarqube.example.com/");

    String manifest = underTest.generateManifest("SonarQube", GithubAppManifestGenerator.SETTINGS_PATH);

    JsonObject json = JsonParser.parseString(manifest).getAsJsonObject();
    assertThat(json.get("name").getAsString()).isEqualTo("SonarQube");
    assertThat(json.get("url").getAsString()).isEqualTo("https://sonarqube.example.com");
    assertThat(json.get("redirect_url").getAsString()).isEqualTo("https://sonarqube.example.com/github/manifest/callback");
    assertThat(json.getAsJsonArray("callback_urls")).hasSize(1);
    assertThat(json.getAsJsonArray("callback_urls").get(0).getAsString()).isEqualTo("https://sonarqube.example.com");
    assertThat(json.get("setup_url").getAsString()).isEqualTo("https://sonarqube.example.com/admin/settings?category=almintegration");
    assertThat(json.get("public").getAsBoolean()).isTrue();
    assertThat(json.get("request_oauth_on_install").getAsBoolean()).isTrue();
    assertThat(json.getAsJsonArray("default_events")).isEmpty();

    JsonObject permissions = json.getAsJsonObject("default_permissions");
    assertThat(permissions.get("checks").getAsString()).isEqualTo("write");
    assertThat(permissions.get("pull_requests").getAsString()).isEqualTo("write");
    assertThat(permissions.get("metadata").getAsString()).isEqualTo("read");
    assertThat(permissions.get("administration").getAsString()).isEqualTo("read");
    assertThat(permissions.get("contents").getAsString()).isEqualTo("write");
  }

  @Test
  public void generateManifest_usesGivenSetupPath() {
    when(server.getPublicRootUrl()).thenReturn("https://sonarqube.example.com");

    String manifest = underTest.generateManifest("SonarQube", GithubAppManifestGenerator.AUTH_SETTINGS_PATH);

    JsonObject json = JsonParser.parseString(manifest).getAsJsonObject();
    assertThat(json.get("setup_url").getAsString()).isEqualTo("https://sonarqube.example.com/admin/settings?category=authentication&tab=github");
  }

  @Test
  public void baseUrl_stripsTrailingSlash() {
    when(server.getPublicRootUrl()).thenReturn("https://sonarqube.example.com/");

    assertThat(underTest.baseUrl()).isEqualTo("https://sonarqube.example.com");
  }

  @Test
  public void githubAppCreationUrl_whenNoOrganization_targetsPersonalAccount() {
    assertThat(underTest.githubAppCreationUrl(null)).isEqualTo("https://github.com/settings/apps/new");
    assertThat(underTest.githubAppCreationUrl("")).isEqualTo("https://github.com/settings/apps/new");
  }

  @Test
  public void githubAppCreationUrl_whenOrganization_isPathEncoded() {
    assertThat(underTest.githubAppCreationUrl("my-org")).isEqualTo("https://github.com/organizations/my-org/settings/apps/new");
    assertThat(underTest.githubAppCreationUrl("my org")).isEqualTo("https://github.com/organizations/my%20org/settings/apps/new");
  }
}
