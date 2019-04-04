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
 * This class is not public anymore since version 5.5. It is
 * created by {@link WsClientFactory}
 *
 * @since 5.3
 */
@Generated("sonar-ws-generator")
class DefaultWsClient implements WsClient {

  private final WsConnector wsConnector;

  private final AnalysisReportsService analysisReportsService;
  private final ApplicationsService applicationsService;
  private final AuthenticationService authenticationService;
  private final CeService ceService;
  private final ComponentsService componentsService;
  private final CustomMeasuresService customMeasuresService;
  private final DevelopersService developersService;
  private final DuplicationsService duplicationsService;
  private final EditionsService editionsService;
  private final EmailsService emailsService;
  private final FavoritesService favoritesService;
  private final FavouritesService favouritesService;
  private final GovernanceReportsService governanceReportsService;
  private final IssuesService issuesService;
  private final L10nService l10nService;
  private final LanguagesService languagesService;
  private final MeasuresService measuresService;
  private final MetricsService metricsService;
  private final NavigationService navigationService;
  private final NotificationsService notificationsService;
  private final OrganizationsService organizationsService;
  private final PermissionsService permissionsService;
  private final PluginsService pluginsService;
  private final ProjectAnalysesService projectAnalysesService;
  private final ProjectBadgesService projectBadgesService;
  private final ProjectBranchesService projectBranchesService;
  private final ProjectDumpService projectDumpService;
  private final ProjectLinksService projectLinksService;
  private final ProjectPullRequestsService projectPullRequestsService;
  private final ProjectTagsService projectTagsService;
  private final ProjectsService projectsService;
  private final PropertiesService propertiesService;
  private final QualitygatesService qualitygatesService;
  private final QualityprofilesService qualityprofilesService;
  private final RootsService rootsService;
  private final RulesService rulesService;
  private final ServerService serverService;
  private final SettingsService settingsService;
  private final SourcesService sourcesService;
  private final SupportService supportService;
  private final SystemService systemService;
  private final TimemachineService timemachineService;
  private final UpdatecenterService updatecenterService;
  private final UserGroupsService userGroupsService;
  private final UserPropertiesService userPropertiesService;
  private final UserTokensService userTokensService;
  private final UsersService usersService;
  private final ViewsService viewsService;
  private final WebhooksService webhooksService;
  private final WebservicesService webservicesService;
  private final BatchService batchService;
  private final SecurityReportsService securityReportsService;

  DefaultWsClient(WsConnector wsConnector) {
    this.wsConnector = wsConnector;

    this.analysisReportsService = new AnalysisReportsService(wsConnector);
    this.applicationsService = new ApplicationsService(wsConnector);
    this.authenticationService = new AuthenticationService(wsConnector);
    this.ceService = new CeService(wsConnector);
    this.componentsService = new ComponentsService(wsConnector);
    this.customMeasuresService = new CustomMeasuresService(wsConnector);
    this.developersService = new DevelopersService(wsConnector);
    this.duplicationsService = new DuplicationsService(wsConnector);
    this.editionsService = new EditionsService(wsConnector);
    this.emailsService = new EmailsService(wsConnector);
    this.favoritesService = new FavoritesService(wsConnector);
    this.favouritesService = new FavouritesService(wsConnector);
    this.governanceReportsService = new GovernanceReportsService(wsConnector);
    this.issuesService = new IssuesService(wsConnector);
    this.l10nService = new L10nService(wsConnector);
    this.languagesService = new LanguagesService(wsConnector);
    this.measuresService = new MeasuresService(wsConnector);
    this.metricsService = new MetricsService(wsConnector);
    this.navigationService = new NavigationService(wsConnector);
    this.notificationsService = new NotificationsService(wsConnector);
    this.organizationsService = new OrganizationsService(wsConnector);
    this.permissionsService = new PermissionsService(wsConnector);
    this.pluginsService = new PluginsService(wsConnector);
    this.projectAnalysesService = new ProjectAnalysesService(wsConnector);
    this.projectBadgesService = new ProjectBadgesService(wsConnector);
    this.projectBranchesService = new ProjectBranchesService(wsConnector);
    this.projectDumpService = new ProjectDumpService(wsConnector);
    this.projectLinksService = new ProjectLinksService(wsConnector);
    this.projectPullRequestsService = new ProjectPullRequestsService(wsConnector);
    this.projectTagsService = new ProjectTagsService(wsConnector);
    this.projectsService = new ProjectsService(wsConnector);
    this.propertiesService = new PropertiesService(wsConnector);
    this.qualitygatesService = new QualitygatesService(wsConnector);
    this.qualityprofilesService = new QualityprofilesService(wsConnector);
    this.rootsService = new RootsService(wsConnector);
    this.rulesService = new RulesService(wsConnector);
    this.serverService = new ServerService(wsConnector);
    this.settingsService = new SettingsService(wsConnector);
    this.sourcesService = new SourcesService(wsConnector);
    this.supportService = new SupportService(wsConnector);
    this.systemService = new SystemService(wsConnector);
    this.timemachineService = new TimemachineService(wsConnector);
    this.updatecenterService = new UpdatecenterService(wsConnector);
    this.userGroupsService = new UserGroupsService(wsConnector);
    this.userPropertiesService = new UserPropertiesService(wsConnector);
    this.userTokensService = new UserTokensService(wsConnector);
    this.usersService = new UsersService(wsConnector);
    this.viewsService = new ViewsService(wsConnector);
    this.webhooksService = new WebhooksService(wsConnector);
    this.webservicesService = new WebservicesService(wsConnector);
    this.batchService = new BatchService(wsConnector);
    this.securityReportsService = new SecurityReportsService(wsConnector);
  }

  @Override
  @Deprecated
  public WsConnector wsConnector() {
    return wsConnector;
  }

  @Override
  public AnalysisReportsService analysisReports() {
    return analysisReportsService;
  }

  @Override
  public ApplicationsService applications() {
    return applicationsService;
  }

  @Override
  public AuthenticationService authentication() {
    return authenticationService;
  }

  @Override
  public CeService ce() {
    return ceService;
  }

  @Override
  public ComponentsService components() {
    return componentsService;
  }

  @Override
  public CustomMeasuresService customMeasures() {
    return customMeasuresService;
  }

  @Override
  public DevelopersService developers() {
    return developersService;
  }

  @Override
  public DuplicationsService duplications() {
    return duplicationsService;
  }

  @Override
  public EditionsService editions() {
    return editionsService;
  }

  @Override
  public EmailsService emails() {
    return emailsService;
  }

  @Override
  public FavoritesService favorites() {
    return favoritesService;
  }

  @Override
  public FavouritesService favourites() {
    return favouritesService;
  }

  @Override
  public GovernanceReportsService governanceReports() {
    return governanceReportsService;
  }

  @Override
  public IssuesService issues() {
    return issuesService;
  }

  @Override
  public L10nService l10n() {
    return l10nService;
  }

  @Override
  public LanguagesService languages() {
    return languagesService;
  }

  @Override
  public MeasuresService measures() {
    return measuresService;
  }

  @Override
  public MetricsService metrics() {
    return metricsService;
  }

  @Override
  public NavigationService navigation() {
    return navigationService;
  }

  @Override
  public NotificationsService notifications() {
    return notificationsService;
  }

  @Override
  public OrganizationsService organizations() {
    return organizationsService;
  }

  @Override
  public PermissionsService permissions() {
    return permissionsService;
  }

  @Override
  public PluginsService plugins() {
    return pluginsService;
  }

  @Override
  public ProjectAnalysesService projectAnalyses() {
    return projectAnalysesService;
  }

  @Override
  public ProjectBadgesService projectBadges() {
    return projectBadgesService;
  }

  @Override
  public ProjectBranchesService projectBranches() {
    return projectBranchesService;
  }

  @Override
  public ProjectDumpService projectDump() {
    return projectDumpService;
  }

  @Override
  public ProjectLinksService projectLinks() {
    return projectLinksService;
  }

  @Override
  public ProjectPullRequestsService projectPullRequests() {
    return projectPullRequestsService;
  }

  @Override
  public ProjectTagsService projectTags() {
    return projectTagsService;
  }

  @Override
  public ProjectsService projects() {
    return projectsService;
  }

  @Override
  public PropertiesService properties() {
    return propertiesService;
  }

  @Override
  public QualitygatesService qualitygates() {
    return qualitygatesService;
  }

  @Override
  public QualityprofilesService qualityprofiles() {
    return qualityprofilesService;
  }

  @Override
  public RootsService roots() {
    return rootsService;
  }

  @Override
  public RulesService rules() {
    return rulesService;
  }

  @Override
  public ServerService server() {
    return serverService;
  }

  @Override
  public SettingsService settings() {
    return settingsService;
  }

  @Override
  public SourcesService sources() {
    return sourcesService;
  }

  @Override
  public SupportService support() {
    return supportService;
  }

  @Override
  public SystemService system() {
    return systemService;
  }

  @Override
  public TimemachineService timemachine() {
    return timemachineService;
  }

  @Override
  public UpdatecenterService updatecenter() {
    return updatecenterService;
  }

  @Override
  public UserGroupsService userGroups() {
    return userGroupsService;
  }

  @Override
  public UserPropertiesService userProperties() {
    return userPropertiesService;
  }

  @Override
  public UserTokensService userTokens() {
    return userTokensService;
  }

  @Override
  public UsersService users() {
    return usersService;
  }

  @Override
  public ViewsService views() {
    return viewsService;
  }

  @Override
  public WebhooksService webhooks() {
    return webhooksService;
  }

  @Override
  public WebservicesService webservices() {
    return webservicesService;
  }

  @Override
  public BatchService batch() {
    return batchService;
  }

  @Override
  public SecurityReportsService securityReports() {
    return securityReportsService;
  }

}
