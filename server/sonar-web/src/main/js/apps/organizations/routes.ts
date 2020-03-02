/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { lazyLoadComponent } from 'sonar-ui-common/components/lazyLoadComponent';
import codingRulesRoutes from '../coding-rules/routes';
import qualityGatesRoutes from '../quality-gates/routes';
import qualityProfilesRoutes from '../quality-profiles/routes';
import webhooksRoutes from '../webhooks/routes';

const OrganizationContainer = lazyLoadComponent(() => import('./components/OrganizationContainer'));

const routes = [
  {
    path: ':organizationKey',
    component: lazyLoadComponent(() => import('./components/OrganizationPage')),
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
          {
            indexRoute: {
              component: lazyLoadComponent(() => import('./components/OrganizationProjects'))
            }
          }
        ]
      },
      {
        path: 'issues',
        component: OrganizationContainer,
        childRoutes: [
          {
            indexRoute: {
              component: lazyLoadComponent(() => import('../issues/components/AppContainer'))
            }
          }
        ]
      },
      {
        path: 'rules',
        component: OrganizationContainer,
        childRoutes: codingRulesRoutes
      },
      {
        path: 'members',
        component: lazyLoadComponent(() =>
          import('../organizationMembers/OrganizationMembersContainer')
        )
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
        component: lazyLoadComponent(() => import('./components/OrganizationAccessContainer')),
        childRoutes: [
          {
            path: 'edit',
            component: lazyLoadComponent(() => import('./components/OrganizationEdit'))
          },
          {
            path: 'groups',
            component: lazyLoadComponent(() => import('../groups/components/App'))
          },
          {
            path: 'permissions',
            component: lazyLoadComponent(() => import('../permissions/global/components/App'))
          },
          {
            path: 'permission_templates',
            component: lazyLoadComponent(() => import('../permission-templates/components/App'))
          },
          {
            path: 'projects_management',
            component: lazyLoadComponent(() => import('../projectsManagement/AppContainer'))
          },
          { path: 'webhooks', childRoutes: webhooksRoutes },
          {
            path: 'extension/:pluginKey/:extensionKey',
            component: lazyLoadComponent(() =>
              import('../../app/components/extensions/OrganizationPageExtension')
            )
          }
        ]
      }
    ]
  }
];

export default routes;
