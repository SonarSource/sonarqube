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
package org.sonarqube.ws.tester;

import org.junit.rules.ExternalResource;
import org.sonarqube.ws.AlmSettings;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.almsettings.AlmSettingsService;
import org.sonarqube.ws.client.almsettings.CreateGithubRequest;
import org.sonarqube.ws.client.almsettings.DeleteRequest;

public class AlmSettingsTester extends ExternalResource {

  private final TesterSession session;

  AlmSettingsTester(TesterSession session) {
    this.session = session;
  }

  public void addGitHubAlmSettings(String key) {
    session.wsClient().almSettings().createGithub(new CreateGithubRequest()
      .setClientId("id1")
      .setAppId("app1")
      .setClientSecret("shhh")
      .setKey(key).setPrivateKey("PRIV")
      .setUrl("http://example.org"));
  }

  void deleteAll() {
    AlmSettingsService almSettingsService = session.wsClient().almSettings();
    try {
      AlmSettings.ListDefinitionsWsResponse response = almSettingsService.listDefinitions();
      response.getGithubList().forEach(e -> almSettingsService.delete(new DeleteRequest(e.getKey())));
      response.getAzureList().forEach(e -> almSettingsService.delete(new DeleteRequest(e.getKey())));
      response.getBitbucketList().forEach(e -> almSettingsService.delete(new DeleteRequest(e.getKey())));
      response.getBitbucketcloudList().forEach(e -> almSettingsService.delete(new DeleteRequest(e.getKey())));
      response.getGitlabList().forEach(e -> almSettingsService.delete(new DeleteRequest(e.getKey())));
    } catch (HttpException e) {
      // If server is not at least a developer edition, the ws is not available, nothing to do
      if (e.code() == 404) {
        return;
      }
      throw new IllegalStateException(e);
    }
  }
}
