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
package org.sonarqube.ws.client.gitlab.configuration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.PatchRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.client.WsResponse;

public class GitlabConfigurationService extends BaseService {


  public GitlabConfigurationService(WsConnector wsConnector) {
    super(wsConnector, "api/v2/dop-translation/gitlab-configurations");
  }

  public String saveGitlabConfiguration(GitlabConfiguration gitlabConfiguration) {
    String body = String.format("""
        {
          "enabled": "%s",
          "applicationId": "%s",
          "url": "%s",
          "secret": "%s",
          "synchronizeGroups": "%s",
          "provisioningType": "%s",
          "allowUsersToSignUp": "%s",
          "provisioningToken": "%s",
          "allowedGroups": ["%s"]
        }
        """, gitlabConfiguration.enabled(), gitlabConfiguration.applicationId(), gitlabConfiguration.url(), gitlabConfiguration.secret(),
      gitlabConfiguration.synchronizeGroups(), gitlabConfiguration.provisioningType(), gitlabConfiguration.allowUsersToSignUp(),
      gitlabConfiguration.provisioningToken(), gitlabConfiguration.singleAllowedGroup());
    WsResponse response = call(
      new PostRequest(path()).setBody(body)
    );
    return new Gson().fromJson(response.content(), JsonObject.class).get("id").getAsString();
  }

  public void enableAutoProvisioning(String configId) {
    setProvisioningMode(configId, "AUTO_PROVISIONING");
  }

  public void disableAutoProvisioning(String configId) {
    setProvisioningMode(configId, "JIT");
  }

  private void setProvisioningMode(String configId, String provisioningMode) {
    String body = String.format("""
      { 
        "provisioningType": "%s"
      }
      """, provisioningMode);
    call(
      new PatchRequest(path(configId)).setBody(body).setContentType(APPLICATION_MERGE_PATCH_JSON)
    );
  }
}
