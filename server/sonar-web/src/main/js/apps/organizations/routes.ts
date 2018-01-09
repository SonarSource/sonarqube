/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { RouterState, RedirectFunction } from 'react-router';
import GlobalPermissionsApp from '../permissions/global/components/App';
import OrganizationPageContainer from './components/OrganizationPage';
import OrganizationPageExtension from '../../app/components/extensions/OrganizationPageExtension';
import OrganizationContainer from './components/OrganizationContainer';
import OrganizationProjects from './components/OrganizationProjects';
import OrganizationRules from './components/OrganizationRules';
import OrganizationAdminContainer from './components/OrganizationAdminContainer';
import OrganizationEdit from './components/OrganizationEdit';
import OrganizationGroups from './components/OrganizationGroups';
import OrganizationMembersContainer from './components/OrganizationMembersContainer';
import OrganizationDelete from './components/OrganizationDelete';
import PermissionTemplateApp from '../permission-templates/components/AppContainer';
import ProjectManagementApp from '../projectsManagement/AppContainer';
import qualityGatesRoutes from '../quality-gates/routes';
import qualityProfilesRoutes from '../quality-profiles/routes';
import Issues from '../issues/components/AppContainer';

const routes = [
  {
    path: ':organizationKey',
    component: OrganizationPageContainer,
    childRoutes: [
      {
        indexRoute: {
          onEnter(nextState: RouterState, replace: RedirectFunction) {
            const { params } = nextState;
            replace(`/organizations/${params.organizationKey}/projects`);
          }
        }
      },
      {
        path: 'projects',
        component: OrganizationContainer,
        childRoutes: [{ indexRoute: { component: OrganizationProjects } }]
      },
      {
        path: 'issues',
        component: OrganizationContainer,
        childRoutes: [{ indexRoute: { component: Issues } }]
      },
      {
        path: 'members',
        component: OrganizationMembersContainer
      },
      {
        path: 'rules',
        component: OrganizationRules
      },
      {
        path: 'quality_profiles',
        childRoutes: qualityProfilesRoutes
      },
      {
        path: 'quality_gates',
        component: OrganizationContainer,
        childRoutes: qualityGatesRoutes
      },
      {
        path: 'extension/:pluginKey/:extensionKey',
        component: OrganizationPageExtension
      },
      {
        component: OrganizationAdminContainer,
        childRoutes: [
          { path: 'delete', component: OrganizationDelete },
          { path: 'edit', component: OrganizationEdit },
          { path: 'groups', component: OrganizationGroups },
          { path: 'permissions', component: GlobalPermissionsApp },
          { path: 'permission_templates', component: PermissionTemplateApp },
          { path: 'projects_management', component: ProjectManagementApp }
        ]
      }
    ]
  }
];

export default routes;
