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
 * This class is not public anymore since version 5.5. It is
 * created by {@link WsClientFactory}
 *
 * @since 5.3
 */
class DefaultWsClient implements WsClient {

  private final WsConnector wsConnector;
  private final OrganizationsService organizations;
  private final org.sonarqube.ws.client.permission.PermissionsService permissionsOld;
  private final PermissionsService permissions;
  private final ComponentsService components;
  private final FavoritesService favoritesService;
  private final QualityProfilesService qualityProfilesOld;
  private final QualityprofilesService qualityprofiles;
  private final IssuesService issues;
  private final UsersService usersService;
  private final UserGroupsService userGroupsService;
  private final UserTokensService userTokensService;
  private final QualitygatesService qualityGatesService;
  private final org.sonarqube.ws.client.measure.MeasuresService measuresOld;
  private final MeasuresService measures;
  private final SystemService systemService;
  private final CeService ceService;
  private final RulesService rulesService;
  private final ProjectsService projectsService;
  private final ProjectLinksService projectLinksService;
  private final SettingsService settingsService;
  private final RootsService rootsService;
  private final WebhooksService webhooksService;
  private final ProjectAnalysisService projectAnalysisOld;
  private final ProjectAnalysesService projectAnalyses;
  private final NotificationsService notificationsService;
  private final ProjectBranchesService projectBranchesService;

  DefaultWsClient(WsConnector wsConnector) {
    this.wsConnector = wsConnector;
    this.organizations = new OrganizationsService(wsConnector);
    this.permissionsOld = new org.sonarqube.ws.client.permission.PermissionsService(wsConnector);
    this.permissions = new PermissionsService(wsConnector);
    this.components = new ComponentsService(wsConnector);
    this.favoritesService = new FavoritesService(wsConnector);
    this.qualityProfilesOld = new QualityProfilesService(wsConnector);
    this.qualityprofiles = new QualityprofilesService(wsConnector);
    this.issues = new IssuesService(wsConnector);
    this.usersService = new UsersService(wsConnector);
    this.userGroupsService = new UserGroupsService(wsConnector);
    this.userTokensService = new UserTokensService(wsConnector);
    this.qualityGatesService = new QualitygatesService(wsConnector);
    this.measuresOld = new org.sonarqube.ws.client.measure.MeasuresService(wsConnector);
    this.measures = new MeasuresService(wsConnector);
    this.systemService = new SystemService(wsConnector);
    this.ceService = new CeService(wsConnector);
    this.rulesService = new RulesService(wsConnector);
    this.projectsService = new ProjectsService(wsConnector);
    this.projectLinksService = new ProjectLinksService(wsConnector);
    this.settingsService = new SettingsService(wsConnector);
    this.rootsService = new RootsService(wsConnector);
    this.webhooksService = new WebhooksService(wsConnector);
    this.projectAnalysisOld = new ProjectAnalysisService(wsConnector);
    this.projectAnalyses = new ProjectAnalysesService(wsConnector);
    this.projectBranchesService = new ProjectBranchesService(wsConnector);
    this.notificationsService = new NotificationsService(wsConnector);
  }

  @Override
  public WsConnector wsConnector() {
    return wsConnector;
  }

  @Override
  public OrganizationsService organizations() {
    return organizations;
  }

  @Override
  public org.sonarqube.ws.client.permission.PermissionsService permissionsOld() {
    return this.permissionsOld;
  }

  @Override
  public PermissionsService permissions() {
    return permissions;
  }

  @Override
  public ComponentsService components() {
    return components;
  }

  @Override
  public FavoritesService favorites() {
    return favoritesService;
  }

  @Override
  public QualityProfilesService qualityProfilesOld() {
    return qualityProfilesOld;
  }

  @Override
  public QualityprofilesService qualityProfiles() {
    return qualityprofiles;
  }

  @Override
  public IssuesService issues() {
    return issues;
  }

  @Override
  public UsersService users() {
    return usersService;
  }

  @Override
  public UserGroupsService userGroups() {
    return userGroupsService;
  }

  @Override
  public UserTokensService userTokens() {
    return userTokensService;
  }

  @Override
  public QualitygatesService qualityGates() {
    return qualityGatesService;
  }

  @Override
  public org.sonarqube.ws.client.measure.MeasuresService measuresOld() {
    return measuresOld;
  }

  @Override
  public MeasuresService measures() {
    return measures;
  }

  @Override
  public SystemService system() {
    return systemService;
  }

  @Override
  public CeService ce() {
    return ceService;
  }

  @Override
  public RulesService rules() {
    return rulesService;
  }

  @Override
  public ProjectsService projects() {
    return projectsService;
  }

  @Override
  public ProjectLinksService projectLinks() {
    return projectLinksService;
  }

  @Override
  public SettingsService settings() {
    return settingsService;
  }

  @Override
  public RootsService roots() {
    return rootsService;
  }

  @Override
  public WebhooksService webhooks() {
    return webhooksService;
  }

  @Override
  public ProjectAnalysisService projectAnalysisOld() {
    return projectAnalysisOld;
  }

  @Override
  public ProjectAnalysesService projectAnalyses() {
    return projectAnalyses;
  }

  @Override
  public ProjectBranchesService projectBranches() {
    return projectBranchesService;
  }

  @Override
  public NotificationsService notifications() {
    return notificationsService;
  }
}
