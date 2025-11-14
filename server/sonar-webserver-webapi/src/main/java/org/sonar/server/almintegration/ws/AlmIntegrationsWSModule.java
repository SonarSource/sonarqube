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
package org.sonar.server.almintegration.ws;

import org.sonar.core.platform.Module;
import org.sonar.server.almintegration.ws.azure.ImportAzureProjectAction;
import org.sonar.server.almintegration.ws.azure.ListAzureProjectsAction;
import org.sonar.server.almintegration.ws.azure.SearchAzureReposAction;
import org.sonar.server.almintegration.ws.bitbucketcloud.ImportBitbucketCloudRepoAction;
import org.sonar.server.almintegration.ws.bitbucketcloud.SearchBitbucketCloudReposAction;
import org.sonar.server.almintegration.ws.bitbucketserver.ImportBitbucketServerProjectAction;
import org.sonar.server.almintegration.ws.bitbucketserver.ListBitbucketServerProjectsAction;
import org.sonar.server.almintegration.ws.bitbucketserver.SearchBitbucketServerReposAction;
import org.sonar.server.almintegration.ws.github.GetGithubClientIdAction;
import org.sonar.server.almintegration.ws.github.ImportGithubProjectAction;
import org.sonar.server.almintegration.ws.github.ListGithubOrganizationsAction;
import org.sonar.server.almintegration.ws.github.ListGithubRepositoriesAction;
import org.sonar.server.almintegration.ws.github.config.CheckAction;
import org.sonar.server.almintegration.ws.gitlab.ImportGitLabProjectAction;
import org.sonar.server.almintegration.ws.gitlab.SearchGitlabReposAction;

public class AlmIntegrationsWSModule extends Module {
  @Override
  protected void configureModule() {
    add(
      CheckPatAction.class,
      CheckAction.class,
      SetPatAction.class,
      ImportBitbucketServerProjectAction.class,
      ImportBitbucketCloudRepoAction.class,
      ListBitbucketServerProjectsAction.class,
      SearchBitbucketServerReposAction.class,
      SearchBitbucketCloudReposAction.class,
      GetGithubClientIdAction.class,
      ImportGithubProjectAction.class,
      ListGithubOrganizationsAction.class,
      ListGithubRepositoriesAction.class,
      ImportGitLabProjectAction.class,
      SearchGitlabReposAction.class,
      ImportAzureProjectAction.class,
      ListAzureProjectsAction.class,
      SearchAzureReposAction.class,
      AlmIntegrationsWs.class);
  }
}
