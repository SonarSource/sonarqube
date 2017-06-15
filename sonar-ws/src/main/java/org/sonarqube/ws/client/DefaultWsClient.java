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
import org.sonarqube.ws.client.component.ComponentsService;
import org.sonarqube.ws.client.favorite.FavoritesService;
import org.sonarqube.ws.client.issue.IssuesService;
import org.sonarqube.ws.client.measure.MeasuresService;
import org.sonarqube.ws.client.organization.OrganizationService;
import org.sonarqube.ws.client.permission.PermissionsService;
import org.sonarqube.ws.client.project.ProjectsService;
import org.sonarqube.ws.client.projectanalysis.ProjectAnalysisService;
import org.sonarqube.ws.client.projectlinks.ProjectLinksService;
import org.sonarqube.ws.client.qualitygate.QualityGatesService;
import org.sonarqube.ws.client.qualityprofile.QualityProfilesService;
import org.sonarqube.ws.client.root.RootsService;
import org.sonarqube.ws.client.rule.RulesService;
import org.sonarqube.ws.client.setting.SettingsService;
import org.sonarqube.ws.client.system.SystemService;
import org.sonarqube.ws.client.user.UsersService;
import org.sonarqube.ws.client.usergroup.UserGroupsService;
import org.sonarqube.ws.client.usertoken.UserTokensService;
import org.sonarqube.ws.client.webhook.WebhooksService;

/**
 * This class is not public anymore since version 5.5. It is
 * created by {@link WsClientFactory}
 *
 * @since 5.3
 */
class DefaultWsClient implements WsClient {

  private final WsConnector wsConnector;
  private final OrganizationService organizations;
  private final PermissionsService permissionsService;
  private final ComponentsService componentsService;
  private final FavoritesService favoritesService;
  private final QualityProfilesService qualityProfilesService;
  private final IssuesService issuesService;
  private final UsersService usersService;
  private final UserGroupsService userGroupsService;
  private final UserTokensService userTokensService;
  private final QualityGatesService qualityGatesService;
  private final MeasuresService measuresService;
  private final SystemService systemService;
  private final CeService ceService;
  private final RulesService rulesService;
  private final ProjectsService projectsService;
  private final ProjectLinksService projectLinksService;
  private final SettingsService settingsService;
  private final RootsService rootsService;
  private final WebhooksService webhooksService;
  private final ProjectAnalysisService projectAnalysisService;

  DefaultWsClient(WsConnector wsConnector) {
    this.wsConnector = wsConnector;
    this.organizations = new OrganizationService(wsConnector);
    this.permissionsService = new PermissionsService(wsConnector);
    this.componentsService = new ComponentsService(wsConnector);
    this.favoritesService = new FavoritesService(wsConnector);
    this.qualityProfilesService = new QualityProfilesService(wsConnector);
    this.issuesService = new IssuesService(wsConnector);
    this.usersService = new UsersService(wsConnector);
    this.userGroupsService = new UserGroupsService(wsConnector);
    this.userTokensService = new UserTokensService(wsConnector);
    this.qualityGatesService = new QualityGatesService(wsConnector);
    this.measuresService = new MeasuresService(wsConnector);
    this.systemService = new SystemService(wsConnector);
    this.ceService = new CeService(wsConnector);
    this.rulesService = new RulesService(wsConnector);
    this.projectsService = new ProjectsService(wsConnector);
    this.projectLinksService = new ProjectLinksService(wsConnector);
    this.settingsService = new SettingsService(wsConnector);
    this.rootsService = new RootsService(wsConnector);
    this.webhooksService = new WebhooksService(wsConnector);
    this.projectAnalysisService = new ProjectAnalysisService(wsConnector);
  }

  @Override
  public WsConnector wsConnector() {
    return wsConnector;
  }

  @Override
  public OrganizationService organizations() {
    return organizations;
  }

  @Override
  public PermissionsService permissions() {
    return this.permissionsService;
  }

  @Override
  public ComponentsService components() {
    return componentsService;
  }

  @Override
  public FavoritesService favorites() {
    return favoritesService;
  }

  @Override
  public QualityProfilesService qualityProfiles() {
    return qualityProfilesService;
  }

  @Override
  public IssuesService issues() {
    return issuesService;
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
  public QualityGatesService qualityGates() {
    return qualityGatesService;
  }

  @Override
  public MeasuresService measures() {
    return measuresService;
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
  public ProjectAnalysisService projectAnalysis() {
    return projectAnalysisService;
  }
}
