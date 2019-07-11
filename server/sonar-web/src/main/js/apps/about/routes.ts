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
import { lazyLoad } from 'sonar-ui-common/components/lazyLoad';
import { isSonarCloud } from '../../helpers/system';

const routes = [
  {
    indexRoute: {
      component: lazyLoad(() =>
        isSonarCloud() ? import('./sonarcloud/Home') : import('./components/AboutApp')
      )
    },
    childRoutes: isSonarCloud()
      ? [
          {
            path: 'contact',
            component: lazyLoad(() => import('./sonarcloud/Contact'))
          },
          {
            path: 'pricing',
            component: lazyLoad(() => import('./sonarcloud/Pricing'))
          },
          {
            path: 'sq',
            childRoutes: [
              { indexRoute: { component: lazyLoad(() => import('./sonarcloud/SQHome')) } },
              {
                path: 'as-a-service',
                component: lazyLoad(() => import('./sonarcloud/AsAService'))
              },
              {
                path: 'branch-analysis-and-pr-decoration',
                component: lazyLoad(() => import('./sonarcloud/BranchAnalysis'))
              },
              {
                path: 'sonarlint-integration',
                component: lazyLoad(() => import('./sonarcloud/SonarLintIntegration'))
              },
              {
                path: 'vsts',
                component: lazyLoad(() => import('./sonarcloud/AzureDevOps'))
              }
            ]
          }
        ]
      : []
  }
];

export default routes;
