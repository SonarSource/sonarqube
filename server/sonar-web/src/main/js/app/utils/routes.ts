/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { PlainRoute, RouteComponent } from 'react-router';
import redirects from './redirects';
import DefaultHelmetContainer from '../components/DefaultHelmetContainer';
import LocalizationContainer from '../components/LocalizationContainer';
import MigrationContainer from '../components/MigrationContainer';
import App from '../components/App';
import GlobalContainer from '../components/GlobalContainer';
import SimpleContainer from '../components/SimpleContainer';
import SimpleSessionsContainer from '../../apps/sessions/components/SimpleSessionsContainer';
import Landing from '../components/Landing';
import ProjectAdminContainer from '../components/ProjectAdminContainer';
import ProjectPageExtension from '../components/extensions/ProjectPageExtension';
import ProjectAdminPageExtension from '../components/extensions/ProjectAdminPageExtension';
import PortfoliosPage from '../components/extensions/PortfoliosPage';
import AdminContainer from '../components/AdminContainer';
import GlobalPageExtension from '../components/extensions/GlobalPageExtension';
import GlobalAdminPageExtension from '../components/extensions/GlobalAdminPageExtension';
import DefaultOrganizationContainer from '../components/DefaultOrganizationContainer';
import MyProjectsContainer from '../components/MyProjectsContainer';
import MarkdownHelp from '../components/MarkdownHelp';
import NotFound from '../components/NotFound';
import aboutRoutes from '../../apps/about/routes';
import accountRoutes from '../../apps/account/routes';
import backgroundTasksRoutes from '../../apps/background-tasks/routes';
import codeRoutes from '../../apps/code/routes';
import codingRulesRoutes from '../../apps/coding-rules/routes';
import componentRoutes from '../../apps/component/routes';
import componentMeasuresRoutes from '../../apps/component-measures/routes';
import customMeasuresRoutes from '../../apps/custom-measures/routes';
import groupsRoutes from '../../apps/groups/routes';
import issuesRoutes from '../../apps/issues/routes';
import marketplaceRoutes from '../../apps/marketplace/routes';
import metricsRoutes from '../../apps/metrics/routes';
import overviewRoutes from '../../apps/overview/routes';
import organizationsRoutes from '../../apps/organizations/routes';
import permissionTemplatesRoutes from '../../apps/permission-templates/routes';
import portfolioRoutes from '../../apps/portfolio/routes';
import projectActivityRoutes from '../../apps/projectActivity/routes';
import projectBranchesRoutes from '../../apps/projectBranches/routes';
import projectQualityGateRoutes from '../../apps/projectQualityGate/routes';
import projectQualityProfilesRoutes from '../../apps/projectQualityProfiles/routes';
import Projects from '../../apps/projects/Projects';
import projectsManagementRoutes from '../../apps/projectsManagement/routes';
import qualityGatesRoutes from '../../apps/quality-gates/routes';
import qualityProfilesRoutes from '../../apps/quality-profiles/routes';
import sessionsRoutes from '../../apps/sessions/routes';
import settingsRoutes from '../../apps/settings/routes';
import systemRoutes from '../../apps/system/routes';
import usersRoutes from '../../apps/users/routes';
import webAPIRoutes from '../../apps/web-api/routes';
import { globalPermissionsRoutes, projectPermissionsRoutes } from '../../apps/permissions/routes';
import Deletion from '../../apps/project-admin/deletion/Deletion';
import Links from '../../apps/project-admin/links/Links';
import Key from '../../apps/project-admin/key/Key';
import MaintenanceAppContainer from '../../apps/maintenance/components/MaintenanceAppContainer';
import SetupAppContainer from '../../apps/maintenance/components/SetupAppContainer';

const projestAdminRoute: PlainRoute = nest(ProjectAdminContainer, [
  childRoute('custom_measures', customMeasuresRoutes),
  componentRoute('project/admin/extension/:pluginKey/:extensionKey', ProjectAdminPageExtension),
  childRoute('project/background_tasks', backgroundTasksRoutes),
  childRoute('project/branches', projectBranchesRoutes),
  componentRoute('project/deletion', Deletion),
  componentRoute('project/key', Key),
  componentRoute('project/links', Links),
  childRoute('project/settings', settingsRoutes),
  childRoute('project_roles', projectPermissionsRoutes)
]);

const projectRoutes: PlainRoute = {
  getComponent: () => import('../components/ComponentContainer').then(i => i.default),
  childRoutes: [
    childRoute('code', codeRoutes),
    childRoute('component_measures', componentMeasuresRoutes),
    childRoute('dashboard', overviewRoutes),
    childRoute('portfolio', portfolioRoutes),
    childRoute('project/activity', projectActivityRoutes),
    componentRoute('project/extension/:pluginKey/:extensionKey', ProjectPageExtension),
    childRoute('project/issues', issuesRoutes),
    childRoute('project/quality_gate', projectQualityGateRoutes),
    childRoute('project/quality_profiles', projectQualityProfilesRoutes),
    projestAdminRoute
  ]
};

const adminRoutes: PlainRoute = {
  path: 'admin',
  component: AdminContainer,
  childRoutes: [
    childRoute('background_tasks', backgroundTasksRoutes),
    childRoute('custom_metrics', metricsRoutes),
    componentRoute('extension/:pluginKey/:extensionKey', GlobalAdminPageExtension),
    childRoute('groups', groupsRoutes),
    childRoute('permission_templates', permissionTemplatesRoutes),
    childRoute('roles/global', globalPermissionsRoutes),
    childRoute('permissions', globalPermissionsRoutes),
    childRoute('projects_management', projectsManagementRoutes),
    childRoute('settings', settingsRoutes),
    childRoute('system', systemRoutes),
    childRoute('marketplace', marketplaceRoutes),
    childRoute('users', usersRoutes)
  ]
};

export default [
  ...redirects,
  componentRoute('markdown/help', MarkdownHelp),
  nest(DefaultHelmetContainer, [
    nest(LocalizationContainer, [
      nest(SimpleContainer, [
        componentRoute('maintenance', MaintenanceAppContainer),
        componentRoute('setup', SetupAppContainer)
      ]),
      nest(LocalizationContainer, [
        nest(MigrationContainer, [
          nest(SimpleSessionsContainer, [childRoute('/sessions', sessionsRoutes)]),
          {
            path: '/',
            component: App,
            indexRoute: { component: Landing },
            childRoutes: [
              nest(GlobalContainer, [
                childRoute('about', aboutRoutes),
                childRoute('account', accountRoutes),
                childRoute('component', componentRoutes),
                childRoute('organizations', organizationsRoutes),
                childRoute('web_api', webAPIRoutes),
                projectRoutes,

                componentRoute('/projects/favorite', MyProjectsContainer),

                nest(DefaultOrganizationContainer, [
                  childRoute('coding_rules', codingRulesRoutes),
                  componentRoute('extension/:pluginKey/:extensionKey', GlobalPageExtension),
                  childRoute('issues', issuesRoutes),
                  componentRoute('projects', Projects),
                  childRoute('quality_gates', qualityGatesRoutes),
                  componentRoute('portfolios', PortfoliosPage),
                  childRoute('profiles', qualityProfilesRoutes),
                  adminRoutes
                ])
              ]),
              componentRoute('not_found', NotFound),
              componentRoute('*', NotFound)
            ]
          }
        ])
      ])
    ])
  ])
];

function nest(component: RouteComponent, childRoutes: PlainRoute[]): PlainRoute {
  return { component, childRoutes };
}

function childRoute(path: string, childRoutes: PlainRoute[]): PlainRoute {
  return { path, childRoutes };
}

function componentRoute(path: string, component: RouteComponent): PlainRoute {
  return { path, component };
}
