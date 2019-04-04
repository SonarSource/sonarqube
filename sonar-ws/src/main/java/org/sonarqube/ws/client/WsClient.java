/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import javax.annotation.Generated;
import org.sonarqube.ws.client.analysisreports.AnalysisReportsService;
import org.sonarqube.ws.client.applications.ApplicationsService;
import org.sonarqube.ws.client.authentication.AuthenticationService;
import org.sonarqube.ws.client.batch.BatchService;
import org.sonarqube.ws.client.ce.CeService;
import org.sonarqube.ws.client.components.ComponentsService;
import org.sonarqube.ws.client.custommeasures.CustomMeasuresService;
import org.sonarqube.ws.client.developers.DevelopersService;
import org.sonarqube.ws.client.duplications.DuplicationsService;
import org.sonarqube.ws.client.editions.EditionsService;
import org.sonarqube.ws.client.emails.EmailsService;
import org.sonarqube.ws.client.favorites.FavoritesService;
import org.sonarqube.ws.client.favourites.FavouritesService;
import org.sonarqube.ws.client.governancereports.GovernanceReportsService;
import org.sonarqube.ws.client.issues.IssuesService;
import org.sonarqube.ws.client.l10n.L10nService;
import org.sonarqube.ws.client.languages.LanguagesService;
import org.sonarqube.ws.client.measures.MeasuresService;
import org.sonarqube.ws.client.metrics.MetricsService;
import org.sonarqube.ws.client.navigation.NavigationService;
import org.sonarqube.ws.client.notifications.NotificationsService;
import org.sonarqube.ws.client.organizations.OrganizationsService;
import org.sonarqube.ws.client.permissions.PermissionsService;
import org.sonarqube.ws.client.plugins.PluginsService;
import org.sonarqube.ws.client.projectanalyses.ProjectAnalysesService;
import org.sonarqube.ws.client.projectbadges.ProjectBadgesService;
import org.sonarqube.ws.client.projectbranches.ProjectBranchesService;
import org.sonarqube.ws.client.projectdump.ProjectDumpService;
import org.sonarqube.ws.client.projectlinks.ProjectLinksService;
import org.sonarqube.ws.client.projectpullrequests.ProjectPullRequestsService;
import org.sonarqube.ws.client.projects.ProjectsService;
import org.sonarqube.ws.client.projecttags.ProjectTagsService;
import org.sonarqube.ws.client.properties.PropertiesService;
import org.sonarqube.ws.client.qualitygates.QualitygatesService;
import org.sonarqube.ws.client.qualityprofiles.QualityprofilesService;
import org.sonarqube.ws.client.roots.RootsService;
import org.sonarqube.ws.client.rules.RulesService;
import org.sonarqube.ws.client.securityreports.SecurityReportsService;
import org.sonarqube.ws.client.server.ServerService;
import org.sonarqube.ws.client.settings.SettingsService;
import org.sonarqube.ws.client.sources.SourcesService;
import org.sonarqube.ws.client.support.SupportService;
import org.sonarqube.ws.client.system.SystemService;
import org.sonarqube.ws.client.timemachine.TimemachineService;
import org.sonarqube.ws.client.updatecenter.UpdatecenterService;
import org.sonarqube.ws.client.usergroups.UserGroupsService;
import org.sonarqube.ws.client.userproperties.UserPropertiesService;
import org.sonarqube.ws.client.users.UsersService;
import org.sonarqube.ws.client.usertokens.UserTokensService;
import org.sonarqube.ws.client.views.ViewsService;
import org.sonarqube.ws.client.webhooks.WebhooksService;
import org.sonarqube.ws.client.webservices.WebservicesService;

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
@Generated("https://github.com/SonarSource/sonar-ws-generator")
public interface WsClient {

  WsConnector wsConnector();

  AnalysisReportsService analysisReports();

  ApplicationsService applications();

  AuthenticationService authentication();

  CeService ce();

  ComponentsService components();

  CustomMeasuresService customMeasures();

  DevelopersService developers();

  DuplicationsService duplications();

  EditionsService editions();

  EmailsService emails();

  FavoritesService favorites();

  FavouritesService favourites();

  GovernanceReportsService governanceReports();

  IssuesService issues();

  L10nService l10n();

  LanguagesService languages();

  MeasuresService measures();

  MetricsService metrics();

  NavigationService navigation();

  NotificationsService notifications();

  OrganizationsService organizations();

  PermissionsService permissions();

  PluginsService plugins();

  ProjectAnalysesService projectAnalyses();

  ProjectBadgesService projectBadges();

  ProjectBranchesService projectBranches();

  ProjectDumpService projectDump();

  ProjectLinksService projectLinks();

  ProjectPullRequestsService projectPullRequests();

  ProjectTagsService projectTags();

  ProjectsService projects();

  PropertiesService properties();

  QualitygatesService qualitygates();

  QualityprofilesService qualityprofiles();

  RootsService roots();

  RulesService rules();

  ServerService server();

  SettingsService settings();

  SourcesService sources();

  SupportService support();

  SystemService system();

  TimemachineService timemachine();

  UpdatecenterService updatecenter();

  UserGroupsService userGroups();

  UserPropertiesService userProperties();

  UserTokensService userTokens();

  UsersService users();

  ViewsService views();

  WebhooksService webhooks();

  WebservicesService webservices();

  BatchService batch();

  SecurityReportsService securityReports();
}
