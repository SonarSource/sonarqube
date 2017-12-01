/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client;

import org.sonarqube.ws.client.ce.CeService;
import org.sonarqube.ws.client.components.ComponentsService;
import org.sonarqube.ws.client.favorites.FavoritesService;
import org.sonarqube.ws.client.issues.IssuesService;
import org.sonarqube.ws.client.measures.MeasuresService;
import org.sonarqube.ws.client.notifications.NotificationsService;
import org.sonarqube.ws.client.organizations.OrganizationsService;
import org.sonarqube.ws.client.permissions.PermissionsService;
import org.sonarqube.ws.client.project.ProjectsService;
import org.sonarqube.ws.client.projectanalyses.ProjectAnalysesService;
import org.sonarqube.ws.client.projectanalysis.ProjectAnalysisService;
import org.sonarqube.ws.client.projectbranches.ProjectBranchesService;
import org.sonarqube.ws.client.projectlinks.ProjectLinksService;
import org.sonarqube.ws.client.qualitygates.QualitygatesService;
import org.sonarqube.ws.client.qualityprofile.QualityProfilesService;
import org.sonarqube.ws.client.qualityprofiles.QualityprofilesService;
import org.sonarqube.ws.client.roots.RootsService;
import org.sonarqube.ws.client.rules.RulesService;
import org.sonarqube.ws.client.settings.SettingsService;
import org.sonarqube.ws.client.system.SystemService;
import org.sonarqube.ws.client.user.UsersService;
import org.sonarqube.ws.client.usergroups.UserGroupsService;
import org.sonarqube.ws.client.usertokens.UserTokensService;
import org.sonarqube.ws.client.webhooks.WebhooksService;

/**
 * Allows to request the web services of SonarQube server. Instance is provided by
 * {@link WsClientFactory}.
 *
 * <p>
 * Usage:
 * <pre>
 *   HttpConnector httpConnector = HttpConnector.newBuilder()
 *     .url("http://localhost:9000")
 *     .credentials("admin", "admin")
 *     .build();
 *   WsClient wsClient = WsClientFactories.getDefault().newClient(httpConnector);
 *   wsClient.issues().search(issueRequest);
 * </pre>
 * </p>
 *
 * @since 5.3
 */
public interface WsClient {

  OrganizationsService organizations();

  ComponentsService components();

  FavoritesService favorites();

  IssuesService issues();

  NotificationsService notifications();

  /**
   * @deprecated since 7.0 use {@link #permissions()} instead
   */
  @Deprecated
  org.sonarqube.ws.client.permission.PermissionsService permissionsOld();

  PermissionsService permissions();

  /**
   * @deprecated since 7.0 use {@link #qualityProfiles()} instead
   */
  @Deprecated
  QualityProfilesService qualityProfilesOld();

  QualityprofilesService qualityProfiles();

  UsersService users();

  UserGroupsService userGroups();

  UserTokensService userTokens();

  QualitygatesService qualityGates();

  /**
   * @deprecated since 7.0 use {@link #measures()} instead
   */
  @Deprecated
  org.sonarqube.ws.client.measure.MeasuresService measuresOld();

  MeasuresService measures();

  SystemService system();

  CeService ce();

  RulesService rules();

  WsConnector wsConnector();

  /**
   * @since 5.5
   */
  ProjectsService projects();

  /**
   * @since 6.1
   */
  ProjectLinksService projectLinks();

  /**
   * @since 6.1
   */
  SettingsService settings();

  /**
   * @since 6.2
   */
  RootsService roots();

  /**
   * @since 6.2
   */
  WebhooksService webhooks();

  /**
   * @since 6.3
   * @deprecated since 7.0 use {@link #projectAnalyses()} instead
   */
  @Deprecated
  ProjectAnalysisService projectAnalysisOld();

  ProjectAnalysesService projectAnalyses();

  /**
   * @since 6.6>
   */
  ProjectBranchesService projectBranches();
}
