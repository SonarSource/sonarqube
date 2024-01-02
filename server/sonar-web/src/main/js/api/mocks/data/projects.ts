/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

/* eslint-disable local-rules/use-metrickey-enum */

import { ComponentQualifier, Visibility } from '../../../types/component';
import { ComponentRaw } from '../../components';

export function mockProjects(): ComponentRaw[] {
  return [
    {
      key: 'linux-autotools-gitlab-ci-sq_AYAYuaB5_scXWD3WLpNC',
      name: 'linux-autotools-gitlab-ci-sq',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-21T03:50:39+0000',
      tags: [],
      visibility: Visibility.Public,
      leakPeriodDate: '2023-06-22T14:30:22+0000',
    },
    {
      key: 'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus',
      name: 'linux-cmake-gitlab-ci-vulnerability-reports-sq',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-16T08:23:35+0000',
      tags: [],
      visibility: Visibility.Public,
      leakPeriodDate: '2023-08-11T15:24:00+0000',
    },
    {
      key: 'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9',
      name: 'macos-cmake-azure-sq',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-21T07:42:10+0000',
      tags: [],
      visibility: Visibility.Public,
      leakPeriodDate: '2022-04-29T14:32:34+0000',
    },
    {
      key: 'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK',
      name: 'macos-cmake-compdb-gh-actions-sq',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-21T03:29:41+0000',
      tags: [],
      visibility: Visibility.Public,
      leakPeriodDate: '2022-05-23T03:47:21+0000',
    },
    {
      key: 'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_',
      name: 'macos-cmake-gh-actions-sq',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-21T03:29:57+0000',
      tags: [],
      visibility: Visibility.Public,
      leakPeriodDate: '2022-04-29T14:31:56+0000',
    },
    {
      key: 'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua',
      name: 'macos-xcode-azure-sq',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-21T03:30:16+0000',
      tags: [],
      visibility: Visibility.Public,
      leakPeriodDate: '2022-04-29T14:32:59+0000',
    },
    {
      key: 'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ',
      name: 'macos-xcode-gh-actions-sq',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-21T03:30:20+0000',
      tags: [],
      visibility: Visibility.Public,
      leakPeriodDate: '2022-04-29T14:32:54+0000',
    },
    {
      key: 'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc',
      name: 'macos-xcode-otherci-sq',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-21T03:30:00+0000',
      tags: [],
      visibility: Visibility.Public,
      leakPeriodDate: '2022-08-23T13:58:25+0000',
    },
    {
      key: 'org.sonarsource.orchestrator:orchestrator-parent',
      name: 'Orchestrator :: Parent',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-21T05:06:09+0000',
      tags: ['platform'],
      visibility: Visibility.Public,
      leakPeriodDate: '2023-08-15T05:06:15+0000',
    },
    {
      key: 'org.sonarsource.python:python',
      name: 'Python',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-21T08:25:53+0000',
      tags: [],
      visibility: Visibility.Public,
      leakPeriodDate: '2023-08-03T09:08:44+0000',
    },
    {
      key: 'rspec-frontend',
      name: 'rspec-frontend',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-21T09:25:03+0000',
      tags: [],
      visibility: Visibility.Public,
      leakPeriodDate: '2022-01-14T16:10:11+0000',
      isFavorite: true,
    },
    {
      key: 'org.sonarsource.api.plugin:sonar-plugin-api',
      name: 'sonar-plugin-api',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-21T00:05:56+0000',
      tags: ['sonarqube'],
      visibility: Visibility.Public,
      leakPeriodDate: '2023-08-10T12:28:45+0000',
    },
    {
      key: 'org.sonarsource.javascript:javascript',
      name: 'SonarJS',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-21T09:52:44+0000',
      tags: [],
      visibility: Visibility.Public,
      leakPeriodDate: '2023-08-07T00:10:25+0000',
    },
    {
      key: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
      name: 'SonarLint Core',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-08T13:51:31+0000',
      tags: ['sonarlint'],
      visibility: Visibility.Public,
      leakPeriodDate: '2023-08-07T08:44:02+0000',
    },
    {
      key: 'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent',
      name: 'SonarLint Daemon',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2022-03-15T07:59:51+0000',
      tags: [],
      visibility: Visibility.Public,
      leakPeriodDate: '2020-07-10T12:07:28+0000',
    },
    {
      key: 'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent',
      name: 'SonarLint for Eclipse',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-18T12:14:54+0000',
      tags: ['sonarlint'],
      visibility: Visibility.Public,
      leakPeriodDate: '2023-07-24T20:24:58+0000',
    },
    {
      key: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
      name: 'SonarLint for IntelliJ IDEA',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-17T08:46:18+0000',
      tags: ['sonarlint'],
      visibility: Visibility.Public,
      leakPeriodDate: '2023-08-14T09:50:38+0000',
    },
    {
      key: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
      name: 'SonarLint for VSCode',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-15T08:16:48+0000',
      tags: ['sonarlint'],
      visibility: Visibility.Public,
      leakPeriodDate: '2023-08-03T09:01:23+0000',
    },
    {
      key: 'sonarlint-omnisharp-dotnet',
      name: 'sonarlint-omnisharp-dotnet',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-14T08:44:19+0000',
      tags: [],
      visibility: Visibility.Public,
      leakPeriodDate: '2023-07-26T08:13:47+0000',
    },
    {
      key: 'sonarqube',
      name: 'SonarQube',
      qualifier: ComponentQualifier.Project,
      analysisDate: '2023-08-16T20:33:15+0000',
      tags: [],
      visibility: Visibility.Public,
      leakPeriodDate: '2023-06-19T20:29:31+0000',
      isFavorite: true,
    },
  ];
}

export function mockFacets() {
  return [
    {
      property: 'coverage',
      values: [
        {
          val: 'NO_DATA',
          count: 0,
        },
        {
          val: '*-30.0',
          count: 20,
        },
        {
          val: '30.0-50.0',
          count: 2,
        },
        {
          val: '50.0-70.0',
          count: 3,
        },
        {
          val: '70.0-80.0',
          count: 1,
        },
        {
          val: '80.0-*',
          count: 8,
        },
      ],
    },
    {
      property: 'alert_status',
      values: [
        {
          val: 'ERROR',
          count: 0,
        },
        {
          val: 'OK',
          count: 34,
        },
      ],
    },
    {
      property: 'reliability_rating',
      values: [
        {
          val: '1',
          count: 35,
        },
        {
          val: '2',
          count: 0,
        },
        {
          val: '3',
          count: 30,
        },
        {
          val: '4',
          count: 4,
        },
        {
          val: '5',
          count: 0,
        },
      ],
    },
    {
      property: 'duplicated_lines_density',
      values: [
        {
          val: '*-3.0',
          count: 34,
        },
        {
          val: '3.0-5.0',
          count: 0,
        },
        {
          val: '5.0-10.0',
          count: 0,
        },
        {
          val: '10.0-20.0',
          count: 0,
        },
        {
          val: '20.0-*',
          count: 0,
        },
        {
          val: 'NO_DATA',
          count: 0,
        },
      ],
    },
    {
      property: 'languages',
      values: [
        {
          val: 'cpp',
          count: 20,
        },
        {
          val: 'java',
          count: 11,
        },
        {
          val: 'xml',
          count: 6,
        },
        {
          val: 'ts',
          count: 4,
        },
        {
          val: 'css',
          count: 2,
        },
        {
          val: 'web',
          count: 2,
        },
        {
          val: 'cs',
          count: 1,
        },
        {
          val: 'js',
          count: 1,
        },
        {
          val: 'kotlin',
          count: 1,
        },
        {
          val: 'py',
          count: 1,
        },
      ],
    },
    {
      property: 'security_rating',
      values: [
        {
          val: '1',
          count: 34,
        },
        {
          val: '2',
          count: 0,
        },
        {
          val: '3',
          count: 0,
        },
        {
          val: '4',
          count: 0,
        },
        {
          val: '5',
          count: 0,
        },
      ],
    },
    {
      property: 'qualifier',
      values: [
        {
          val: 'APP',
          count: 0,
        },
        {
          val: 'TRK',
          count: 34,
        },
      ],
    },
    {
      property: 'ncloc',
      values: [
        {
          val: '*-1000.0',
          count: 21,
        },
        {
          val: '1000.0-10000.0',
          count: 5,
        },
        {
          val: '10000.0-100000.0',
          count: 7,
        },
        {
          val: '100000.0-500000.0',
          count: 1,
        },
        {
          val: '500000.0-*',
          count: 0,
        },
      ],
    },
    {
      property: 'security_review_rating',
      values: [
        {
          val: '1',
          count: 30,
        },
        {
          val: '2',
          count: 0,
        },
        {
          val: '3',
          count: 1,
        },
        {
          val: '4',
          count: 0,
        },
        {
          val: '5',
          count: 3,
        },
      ],
    },
    {
      property: 'tags',
      values: [
        {
          val: 'sonarlint',
          count: 4,
        },
        {
          val: 'sonarqube',
          count: 2,
        },
        {
          val: 'platform',
          count: 1,
        },
        {
          val: 'scanner',
          count: 1,
        },
      ],
    },
    {
      property: 'sqale_rating',
      values: [
        {
          val: '1',
          count: 14,
        },
        {
          val: '2',
          count: 20,
        },
        {
          val: '3',
          count: 0,
        },
        {
          val: '4',
          count: 0,
        },
        {
          val: '5',
          count: 0,
        },
      ],
    },
  ];
}

export function mockProjectMeasures() {
  return {
    'org.sonarsource.orchestrator:orchestrator-parent': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'org.sonarsource.orchestrator:orchestrator-parent',
      },
      bugs: {
        metric: 'bugs',
        value: '8',
        component: 'org.sonarsource.orchestrator:orchestrator-parent',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '248',
        component: 'org.sonarsource.orchestrator:orchestrator-parent',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '89.2',
        component: 'org.sonarsource.orchestrator:orchestrator-parent',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.0',
        component: 'org.sonarsource.orchestrator:orchestrator-parent',
        bestValue: true,
      },
      new_lines: {
        metric: 'new_lines',
        value: '69',
        component: 'org.sonarsource.orchestrator:orchestrator-parent',
      },
      ncloc: {
        metric: 'ncloc',
        value: '6669',
        component: 'org.sonarsource.orchestrator:orchestrator-parent',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'java=6273;xml=396',
        component: 'org.sonarsource.orchestrator:orchestrator-parent',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '4.0',
        component: 'org.sonarsource.orchestrator:orchestrator-parent',
        bestValue: false,
      },
      security_hotspots_reviewed: {
        metric: 'security_hotspots_reviewed',
        value: '0.0',
        component: 'org.sonarsource.orchestrator:orchestrator-parent',
        bestValue: false,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'org.sonarsource.orchestrator:orchestrator-parent',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '5.0',
        component: 'org.sonarsource.orchestrator:orchestrator-parent',
        bestValue: false,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '1.0',
        component: 'org.sonarsource.orchestrator:orchestrator-parent',
        bestValue: true,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'org.sonarsource.orchestrator:orchestrator-parent',
        bestValue: true,
      },
    },
    'org.sonarsource.python:python': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'org.sonarsource.python:python',
      },
      bugs: {
        metric: 'bugs',
        value: '5',
        component: 'org.sonarsource.python:python',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '396',
        component: 'org.sonarsource.python:python',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '98.4',
        component: 'org.sonarsource.python:python',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.2',
        component: 'org.sonarsource.python:python',
        bestValue: false,
      },
      new_lines: {
        metric: 'new_lines',
        value: '59',
        component: 'org.sonarsource.python:python',
      },
      ncloc: { metric: 'ncloc', value: '46759', component: 'org.sonarsource.python:python' },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'java=44895;py=827;xml=1037',
        component: 'org.sonarsource.python:python',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '4.0',
        component: 'org.sonarsource.python:python',
        bestValue: false,
      },
      security_hotspots_reviewed: {
        metric: 'security_hotspots_reviewed',
        value: '50.0',
        component: 'org.sonarsource.python:python',
        bestValue: false,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'org.sonarsource.python:python',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '3.0',
        component: 'org.sonarsource.python:python',
        bestValue: false,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '1.0',
        component: 'org.sonarsource.python:python',
        bestValue: true,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'org.sonarsource.python:python',
        bestValue: true,
      },
    },
    'org.sonarsource.javascript:javascript': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'org.sonarsource.javascript:javascript',
      },
      bugs: {
        metric: 'bugs',
        value: '3',
        component: 'org.sonarsource.javascript:javascript',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '953',
        component: 'org.sonarsource.javascript:javascript',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '97.6',
        component: 'org.sonarsource.javascript:javascript',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.1',
        component: 'org.sonarsource.javascript:javascript',
        bestValue: false,
      },
      new_lines: {
        metric: 'new_lines',
        value: '189',
        component: 'org.sonarsource.javascript:javascript',
      },
      ncloc: {
        metric: 'ncloc',
        value: '59689',
        component: 'org.sonarsource.javascript:javascript',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'java=14530;ts=29254;web=15905',
        component: 'org.sonarsource.javascript:javascript',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '3.0',
        component: 'org.sonarsource.javascript:javascript',
        bestValue: false,
      },
      security_hotspots_reviewed: {
        metric: 'security_hotspots_reviewed',
        value: '100.0',
        component: 'org.sonarsource.javascript:javascript',
        bestValue: true,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'org.sonarsource.javascript:javascript',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'org.sonarsource.javascript:javascript',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '1.0',
        component: 'org.sonarsource.javascript:javascript',
        bestValue: true,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'org.sonarsource.javascript:javascript',
        bestValue: true,
      },
    },
    'org.sonarsource.sonarlint.core:sonarlint-core-parent': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
      },
      bugs: {
        metric: 'bugs',
        value: '1',
        component: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '443',
        component: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '91.1',
        component: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.7',
        component: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
        bestValue: false,
      },
      new_lines: {
        metric: 'new_lines',
        value: '24',
        component: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
      },
      ncloc: {
        metric: 'ncloc',
        value: '30686',
        component: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'java=28676;xml=2010',
        component: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '3.0',
        component: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
        bestValue: false,
      },
      security_hotspots_reviewed: {
        metric: 'security_hotspots_reviewed',
        value: '100.0',
        component: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
        bestValue: true,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
        bestValue: true,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'org.sonarsource.sonarlint.core:sonarlint-core-parent',
        bestValue: true,
      },
    },
    'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent',
      },
      bugs: {
        metric: 'bugs',
        value: '2',
        component: 'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '51',
        component: 'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '42.9',
        component: 'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '2.0',
        component: 'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent',
        bestValue: false,
      },
      new_lines: {
        metric: 'new_lines',
        value: '38',
        component: 'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent',
      },
      ncloc: {
        metric: 'ncloc',
        value: '938',
        component: 'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'java=710;xml=228',
        component: 'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '3.0',
        component: 'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent',
        bestValue: false,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent',
        bestValue: true,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'org.sonarsource.sonarlint.daemon:sonarlint-daemon-parent',
        bestValue: true,
      },
    },
    'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent',
      },
      bugs: {
        metric: 'bugs',
        value: '15',
        component: 'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '159',
        component: 'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '64.7',
        component: 'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.8',
        component: 'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent',
        bestValue: false,
      },
      new_lines: {
        metric: 'new_lines',
        value: '26',
        component: 'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent',
      },
      ncloc: {
        metric: 'ncloc',
        value: '22236',
        component: 'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'java=21526;xml=710',
        component: 'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '3.0',
        component: 'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent',
        bestValue: false,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent',
        bestValue: true,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'org.sonarsource.sonarlint.eclipse:sonarlint-eclipse-parent',
        bestValue: true,
      },
    },
    'org.sonarsource.sonarlint.intellij:sonarlint-intellij': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
      },
      bugs: {
        metric: 'bugs',
        value: '8',
        component: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '154',
        component: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '35.1',
        component: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.0',
        component: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
        bestValue: true,
      },
      new_lines: {
        metric: 'new_lines',
        value: '76',
        component: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
      },
      ncloc: {
        metric: 'ncloc',
        value: '23676',
        component: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'java=17147;kotlin=6529',
        component: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '3.0',
        component: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
        bestValue: false,
      },
      security_hotspots_reviewed: {
        metric: 'security_hotspots_reviewed',
        value: '100.0',
        component: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
        bestValue: true,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
        bestValue: true,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'org.sonarsource.sonarlint.intellij:sonarlint-intellij',
        bestValue: true,
      },
    },
    'org.sonarsource.sonarlint.vscode:sonarlint-vscode': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
      },
      bugs: {
        metric: 'bugs',
        value: '13',
        component: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '31',
        component: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '66.9',
        component: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.0',
        component: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
        bestValue: true,
      },
      new_lines: {
        metric: 'new_lines',
        value: '4',
        component: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
      },
      ncloc: {
        metric: 'ncloc',
        value: '5704',
        component: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'ts=5704',
        component: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '4.0',
        component: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
        bestValue: false,
      },
      security_hotspots_reviewed: {
        metric: 'security_hotspots_reviewed',
        value: '100.0',
        component: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
        bestValue: true,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '1.0',
        component: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
        bestValue: true,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'org.sonarsource.sonarlint.vscode:sonarlint-vscode',
        bestValue: true,
      },
    },
    sonarqube: {
      alert_status: { metric: 'alert_status', value: 'OK', component: 'sonarqube' },
      bugs: { metric: 'bugs', value: '21', component: 'sonarqube', bestValue: false },
      code_smells: {
        metric: 'code_smells',
        value: '3513',
        component: 'sonarqube',
        bestValue: false,
      },
      coverage: { metric: 'coverage', value: '91.1', component: 'sonarqube', bestValue: false },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.7',
        component: 'sonarqube',
        bestValue: false,
      },
      new_lines: {
        metric: 'new_lines',
        value: '389',
        component: 'sonarqube',
      },
      ncloc: { metric: 'ncloc', value: '338389', component: 'sonarqube' },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'css=7331;java=209272;js=180;ts=121606',
        component: 'sonarqube',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '4.0',
        component: 'sonarqube',
        bestValue: false,
      },
      security_hotspots_reviewed: {
        metric: 'security_hotspots_reviewed',
        value: '100.0',
        component: 'sonarqube',
        bestValue: true,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'sonarqube',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'sonarqube',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '1.0',
        component: 'sonarqube',
        bestValue: true,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'sonarqube',
        bestValue: true,
      },
    },
    'linux-autotools-gitlab-ci-sq_AYAYuaB5_scXWD3WLpNC': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'linux-autotools-gitlab-ci-sq_AYAYuaB5_scXWD3WLpNC',
      },
      bugs: {
        metric: 'bugs',
        value: '1',
        component: 'linux-autotools-gitlab-ci-sq_AYAYuaB5_scXWD3WLpNC',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '8',
        component: 'linux-autotools-gitlab-ci-sq_AYAYuaB5_scXWD3WLpNC',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '0.0',
        component: 'linux-autotools-gitlab-ci-sq_AYAYuaB5_scXWD3WLpNC',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.0',
        component: 'linux-autotools-gitlab-ci-sq_AYAYuaB5_scXWD3WLpNC',
        bestValue: true,
      },
      ncloc: {
        metric: 'ncloc',
        value: '21',
        component: 'linux-autotools-gitlab-ci-sq_AYAYuaB5_scXWD3WLpNC',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'cpp=21',
        component: 'linux-autotools-gitlab-ci-sq_AYAYuaB5_scXWD3WLpNC',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '3.0',
        component: 'linux-autotools-gitlab-ci-sq_AYAYuaB5_scXWD3WLpNC',
        bestValue: false,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'linux-autotools-gitlab-ci-sq_AYAYuaB5_scXWD3WLpNC',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'linux-autotools-gitlab-ci-sq_AYAYuaB5_scXWD3WLpNC',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '2.0',
        component: 'linux-autotools-gitlab-ci-sq_AYAYuaB5_scXWD3WLpNC',
        bestValue: false,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'linux-autotools-gitlab-ci-sq_AYAYuaB5_scXWD3WLpNC',
        bestValue: true,
      },
    },
    'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus',
      },
      bugs: {
        metric: 'bugs',
        value: '1',
        component: 'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '8',
        component: 'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '0.0',
        component: 'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.0',
        component: 'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus',
        bestValue: true,
      },
      new_lines: {
        metric: 'new_lines',
        value: '0',
        component: 'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus',
      },
      ncloc: {
        metric: 'ncloc',
        value: '21',
        component: 'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'cpp=21',
        component: 'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '3.0',
        component: 'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus',
        bestValue: false,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '2.0',
        component: 'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus',
        bestValue: false,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'linux-cmake-gitlab-ci-vulnerability-reports-sq_AYnApJhM3jwBWLzm5nus',
        bestValue: true,
      },
    },
    'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9',
      },
      bugs: {
        metric: 'bugs',
        value: '1',
        component: 'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '8',
        component: 'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '0.0',
        component: 'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.0',
        component: 'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9',
        bestValue: true,
      },
      new_lines: {
        metric: 'new_lines',
        value: '1',
        component: 'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9',
      },
      ncloc: {
        metric: 'ncloc',
        value: '21',
        component: 'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'cpp=21',
        component: 'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '3.0',
        component: 'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9',
        bestValue: false,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '2.0',
        component: 'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9',
        bestValue: false,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'macos-cmake-azure-sq_AYAYt3nkMi_-8diYBjJ9',
        bestValue: true,
      },
    },
    'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK',
      },
      bugs: {
        metric: 'bugs',
        value: '1',
        component: 'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '8',
        component: 'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '0.0',
        component: 'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.0',
        component: 'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK',
        bestValue: true,
      },
      new_lines: {
        metric: 'new_lines',
        value: '4',
        component: 'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK',
      },
      ncloc: {
        metric: 'ncloc',
        value: '21',
        component: 'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'cpp=21',
        component: 'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '3.0',
        component: 'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK',
        bestValue: false,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '2.0',
        component: 'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK',
        bestValue: false,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'macos-cmake-compdb-gh-actions-sq_AYCKCNO5nBwJQJjdVHcK',
        bestValue: true,
      },
    },
    'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_',
      },
      bugs: {
        metric: 'bugs',
        value: '1',
        component: 'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '8',
        component: 'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '0.0',
        component: 'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.0',
        component: 'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_',
        bestValue: true,
      },
      new_lines: {
        metric: 'new_lines',
        value: '3',
        component: 'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_',
      },
      ncloc: {
        metric: 'ncloc',
        value: '21',
        component: 'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'cpp=21',
        component: 'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '3.0',
        component: 'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_',
        bestValue: false,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '2.0',
        component: 'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_',
        bestValue: false,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'macos-cmake-gh-actions-sq_AYAYs22XMi_-8diYBjH_',
        bestValue: true,
      },
    },
    'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua',
      },
      bugs: {
        metric: 'bugs',
        value: '1',
        component: 'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '7',
        component: 'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '0.0',
        component: 'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.0',
        component: 'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua',
        bestValue: true,
      },
      new_lines: {
        metric: 'new_lines',
        value: '3',
        component: 'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua',
      },
      ncloc: {
        metric: 'ncloc',
        value: '21',
        component: 'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'cpp=21',
        component: 'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '3.0',
        component: 'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua',
        bestValue: false,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '2.0',
        component: 'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua',
        bestValue: false,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'macos-xcode-azure-sq_AYAYr_NcQUWaRZD6h1ua',
        bestValue: true,
      },
    },
    'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ',
      },
      bugs: {
        metric: 'bugs',
        value: '1',
        component: 'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '7',
        component: 'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '0.0',
        component: 'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.0',
        component: 'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ',
        bestValue: true,
      },
      new_lines: {
        metric: 'new_lines',
        value: '1',
        component: 'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ',
      },
      ncloc: {
        metric: 'ncloc',
        value: '21',
        component: 'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'cpp=21',
        component: 'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '3.0',
        component: 'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ',
        bestValue: false,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '2.0',
        component: 'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ',
        bestValue: false,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'macos-xcode-gh-actions-sq_AYAYsHe3y0k_ZlpkA-kQ',
        bestValue: true,
      },
    },
    'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc',
      },
      bugs: {
        metric: 'bugs',
        value: '1',
        component: 'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '7',
        component: 'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '0.0',
        component: 'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.0',
        component: 'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc',
        bestValue: true,
      },
      new_lines: {
        metric: 'new_lines',
        value: '19',
        component: 'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc',
      },
      ncloc: {
        metric: 'ncloc',
        value: '21',
        component: 'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'cpp=21',
        component: 'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '3.0',
        component: 'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc',
        bestValue: false,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '2.0',
        component: 'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc',
        bestValue: false,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'macos-xcode-otherci-sq_AYAYrm2RwUGdjBp0BJBc',
        bestValue: true,
      },
    },
    'rspec-frontend': {
      alert_status: { metric: 'alert_status', value: 'OK', component: 'rspec-frontend' },
      bugs: { metric: 'bugs', value: '4', component: 'rspec-frontend', bestValue: false },
      code_smells: {
        metric: 'code_smells',
        value: '59',
        component: 'rspec-frontend',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '88.5',
        component: 'rspec-frontend',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.0',
        component: 'rspec-frontend',
        bestValue: true,
      },
      new_lines: {
        metric: 'new_lines',
        value: '0',
        component: 'rspec-frontend',
      },
      ncloc: { metric: 'ncloc', value: '3128', component: 'rspec-frontend' },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'css=102;ts=1963;web=1063',
        component: 'rspec-frontend',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '4.0',
        component: 'rspec-frontend',
        bestValue: false,
      },
      security_hotspots_reviewed: {
        metric: 'security_hotspots_reviewed',
        value: '100.0',
        component: 'rspec-frontend',
        bestValue: true,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'rspec-frontend',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'rspec-frontend',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '1.0',
        component: 'rspec-frontend',
        bestValue: true,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'rspec-frontend',
        bestValue: true,
      },
    },
    'org.sonarsource.api.plugin:sonar-plugin-api': {
      alert_status: {
        metric: 'alert_status',
        value: 'OK',
        component: 'org.sonarsource.api.plugin:sonar-plugin-api',
      },
      bugs: {
        metric: 'bugs',
        value: '13',
        component: 'org.sonarsource.api.plugin:sonar-plugin-api',
        bestValue: false,
      },
      code_smells: {
        metric: 'code_smells',
        value: '662',
        component: 'org.sonarsource.api.plugin:sonar-plugin-api',
        bestValue: false,
      },
      coverage: {
        metric: 'coverage',
        value: '80.0',
        component: 'org.sonarsource.api.plugin:sonar-plugin-api',
        bestValue: false,
      },
      duplicated_lines_density: {
        metric: 'duplicated_lines_density',
        value: '0.6',
        component: 'org.sonarsource.api.plugin:sonar-plugin-api',
        bestValue: false,
      },
      new_lines: {
        metric: 'new_lines',
        value: '642',
        component: 'org.sonarsource.api.plugin:sonar-plugin-api',
      },
      ncloc: {
        metric: 'ncloc',
        value: '15642',
        component: 'org.sonarsource.api.plugin:sonar-plugin-api',
      },
      ncloc_language_distribution: {
        metric: 'ncloc_language_distribution',
        value: 'java=15642',
        component: 'org.sonarsource.api.plugin:sonar-plugin-api',
      },
      reliability_rating: {
        metric: 'reliability_rating',
        value: '3.0',
        component: 'org.sonarsource.api.plugin:sonar-plugin-api',
        bestValue: false,
      },
      security_hotspots_reviewed: {
        metric: 'security_hotspots_reviewed',
        value: '100.0',
        component: 'org.sonarsource.api.plugin:sonar-plugin-api',
        bestValue: true,
      },
      security_rating: {
        metric: 'security_rating',
        value: '1.0',
        component: 'org.sonarsource.api.plugin:sonar-plugin-api',
        bestValue: true,
      },
      security_review_rating: {
        metric: 'security_review_rating',
        value: '1.0',
        component: 'org.sonarsource.api.plugin:sonar-plugin-api',
        bestValue: true,
      },
      sqale_rating: {
        metric: 'sqale_rating',
        value: '1.0',
        component: 'org.sonarsource.api.plugin:sonar-plugin-api',
        bestValue: true,
      },
      vulnerabilities: {
        metric: 'vulnerabilities',
        value: '0',
        component: 'org.sonarsource.api.plugin:sonar-plugin-api',
        bestValue: true,
      },
    },
  };
}
