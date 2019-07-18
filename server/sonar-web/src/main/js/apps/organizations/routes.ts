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
import { RedirectFunction, RouterState } from 'react-router';
import { lazyLoad } from 'sonar-ui-common/components/lazyLoad';
import codingRulesRoutes from '../coding-rules/routes';
import qualityGatesRoutes from '../quality-gates/routes';
import qualityProfilesRoutes from '../quality-profiles/routes';
import webhooksRoutes from '../webhooks/routes';

const OrganizationContainer = lazyLoad(() => import('./components/OrganizationContainer'));

const routes = [
  {
    path: ':organizationKey',
    component: lazyLoad(() => import('./components/OrganizationPage')),
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
        childRoutes: [
          { indexRoute: { component: lazyLoad(() => import('./components/OrganizationProjects')) } }
        ]
      },
      {
        path: 'issues',
        component: OrganizationContainer,
        childRoutes: [
          { indexRoute: { component: lazyLoad(() => import('../issues/components/AppContainer')) } }
        ]
      },
      {
        path: 'rules',
        component: OrganizationContainer,
        childRoutes: codingRulesRoutes
      },
      {
        path: 'members',
        component: lazyLoad(() => import('../organizationMembers/OrganizationMembersContainer'))
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
        component: lazyLoad(() => import('./components/OrganizationAccessContainer')),
        childRoutes: [
          { path: 'edit', component: lazyLoad(() => import('./components/OrganizationEdit')) },
          { path: 'groups', component: lazyLoad(() => import('../groups/components/App')) },
          {
            path: 'permissions',
            component: lazyLoad(() => import('../permissions/global/components/App'))
          },
          {
            path: 'permission_templates',
            component: lazyLoad(() => import('../permission-templates/components/App'))
          },
          {
            path: 'projects_management',
            component: lazyLoad(() => import('../projectsManagement/AppContainer'))
          },
          { path: 'webhooks', childRoutes: webhooksRoutes },
          {
            path: 'extension/:pluginKey/:extensionKey',
            component: lazyLoad(() =>
              import('../../app/components/extensions/OrganizationPageExtension')
            )
          }
        ]
      }
    ]
  }
];

export default routes;
