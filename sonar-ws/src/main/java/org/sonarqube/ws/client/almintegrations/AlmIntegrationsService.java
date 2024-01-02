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
package org.sonarqube.ws.client.almintegrations;

import javax.annotation.Generated;
import org.sonarqube.ws.AlmIntegrations;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Projects;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class AlmIntegrationsService extends BaseService {

  public AlmIntegrationsService(WsConnector wsConnector) {
    super(wsConnector, "api/alm_integrations");
  }

  /**
   * This is part of the internal API.
   * This is a GET request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/check_pat">Further information about this action online (including a response example)</a>
   * @since 8.2
   */
  public void checkPat(CheckPatRequest request) {
    call(
      new GetRequest(path("check_pat"))
        .setParam("almSetting", request.getAlmSetting())
        .setMediaType(MediaTypes.JSON)
    ).content();
  }

  /**
   * This is part of the internal API.
   * This is a POST request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/import_bitbucketserver_project">Further information about this action online (including a response example)</a>
   * @since 8.2
   */
  public Projects.CreateWsResponse importBitbucketserverProject(ImportBitbucketserverProjectRequest request) {
    return call(
      new PostRequest(path("import_bitbucketserver_project"))
        .setParam("almSetting", request.getAlmSetting())
        .setParam("projectKey", request.getProjectKey())
        .setParam("repositorySlug", request.getRepositorySlug())
        .setMediaType(MediaTypes.JSON),
      Projects.CreateWsResponse.parser());
  }

  /**
   * This is part of the internal API.
   * This is a POST request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/import_bitbucketcloud_project">Further information about this action online (including a response example)</a>
   * @since 8.2
   */
  public Projects.CreateWsResponse importBitbucketcloudProject(ImportBitbucketcloudRepoRequest request) {
    return call(
      new PostRequest(path("import_bitbucketcloud_repo"))
        .setParam("almSetting", request.getAlmSetting())
        .setParam("repositorySlug", request.getRepositorySlug())
        .setMediaType(MediaTypes.JSON),
      Projects.CreateWsResponse.parser());
  }

  /**
   * This is a POST request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/import_gitlab_project">Further information about this action online (including a response example)</a>
   * @since 8.5
   */
  public Projects.CreateWsResponse importGitLabProject(ImportGitLabProjectRequest request) {
    return call(
      new PostRequest(path("import_gitlab_project"))
        .setParam("almSetting", request.getAlmSetting())
        .setParam("gitlabProjectId", request.getGitlabProjectId())
        .setMediaType(MediaTypes.JSON),
      Projects.CreateWsResponse.parser());
  }

  /**
   * This is part of the internal API.
   * This is a POST request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/import_azure_project">Further information about this action online (including a response example)</a>
   * @since 8.6
   */
  public Projects.CreateWsResponse importAzureProject(ImportAzureProjectRequest request) {
    return call(
      new PostRequest(path("import_azure_project"))
        .setParam("almSetting", request.getAlmSetting())
        .setParam("projectName", request.getProjectName())
        .setParam("repositoryName", request.getRepositoryName())
        .setMediaType(MediaTypes.JSON),
      Projects.CreateWsResponse.parser()
    );
  }

  /**
   * This is part of the internal API.
   * This is a GET request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/search_gitlab_repos">Further information about this action online (including a response example)</a>
   * @since 8.5
   */
  public AlmIntegrations.SearchGitlabReposWsResponse searchGitlabRepos(SearchGitlabReposRequest request) {
    return call(
      new GetRequest(path("search_gitlab_repos"))
        .setParam("almSetting", request.getAlmSetting())
        .setParam("projectName", request.getProjectName())
        .setMediaType(MediaTypes.JSON),
      AlmIntegrations.SearchGitlabReposWsResponse.parser());
  }

  /**
   * This is part of the internal API.
   * This is a GET request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/list_azure_projects">Further information about this action online (including a response example)</a>
   * @since 8.2
   */
  public AlmIntegrations.ListAzureProjectsWsResponse listAzureProjects(ListAzureProjectsRequest request) {
    return call(
      new GetRequest(path("list_azure_projects"))
        .setParam("almSetting", request.getAlmSetting())
        .setMediaType(MediaTypes.JSON),
      AlmIntegrations.ListAzureProjectsWsResponse.parser());
  }

  /**
   * This is part of the internal API.
   * This is a GET request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/list_bitbucketserver_projects">Further information about this action online (including a response example)</a>
   * @since 8.2
   */
  public AlmIntegrations.ListBitbucketserverProjectsWsResponse listBitbucketserverProjects(ListBitbucketserverProjectsRequest request) {
    return call(
      new GetRequest(path("list_bitbucketserver_projects"))
        .setParam("almSetting", request.getAlmSetting())
        .setMediaType(MediaTypes.JSON),
      AlmIntegrations.ListBitbucketserverProjectsWsResponse.parser());
  }

  /**
   * This is part of the internal API.
   * This is a GET request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/search_bitbucketserver_repos">Further information about this action online (including a response example)</a>
   * @since 8.2
   */
  public AlmIntegrations.SearchBitbucketserverReposWsResponse searchBitbucketserverRepos(SearchBitbucketserverReposRequest request) {
    return call(
      new GetRequest(path("search_bitbucketserver_repos"))
        .setParam("almSetting", request.getAlmSetting())
        .setParam("projectName", request.getProjectName())
        .setParam("repositoryName", request.getRepositoryName())
        .setMediaType(MediaTypes.JSON),
      AlmIntegrations.SearchBitbucketserverReposWsResponse.parser());
  }

  /**
   * This is part of the internal API.
   * This is a GET request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/search_bitbucketcloud_repos">Further information about this action online (including a response example)</a>
   * @since 8.2
   */
  public AlmIntegrations.SearchBitbucketcloudReposWsResponse searchBitbucketcloudRepos(SearchBitbucketcloudReposRequest request) {
    return call(
      new GetRequest(path("search_bitbucketcloud_repos"))
        .setParam("almSetting", request.getAlmSetting())
        .setMediaType(MediaTypes.JSON),
      AlmIntegrations.SearchBitbucketcloudReposWsResponse.parser());
  }

  /**
   * This is part of the internal API.
   * This is a POST request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/webhook_github">Further information about this action online (including a response example)</a>
   * @since 9.7
   */
  public void sendGitubCodeScanningAlertWebhookPayload(SendGithubCodeScanningAlertWebhookPayloadRequest request) {
    call(
      new PostRequest(path("webhook_github"))
        .setHeader("X-GitHub-Event", request.getGithubEventHeader())
        .setHeader("X-Hub-Signature", request.getGithubSignatureHeader())
        .setHeader("X-Hub-Signature-256", request.getGithubSignature256Header())
        .setHeader("x-github-hook-installation-target-id", request.getGithubAppId())
        .setBody(request.getPayload())
        .setMediaType(MediaTypes.JSON)
    ).content();
  }

  /**
   * This is part of the internal API.
   * This is a POST request.
   *
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/set_pat">Further information about this action online (including a response example)</a>
   * @since 8.2
   */
  public void setPat(SetPatRequest request) {
    call(
      new PostRequest(path("set_pat"))
        .setParam("almSetting", request.getAlmSetting())
        .setParam("pat", request.getPat())
        .setParam("username", request.getUsername())
        .setMediaType(MediaTypes.JSON)
    ).content();
  }
}
