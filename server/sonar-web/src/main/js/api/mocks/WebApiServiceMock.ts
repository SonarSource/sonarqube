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

import { cloneDeep } from 'lodash';
import { OpenAPIV3 } from 'openapi-types';
import { mockAction } from '../../helpers/mocks/webapi';
import { MetricKey } from '../../sonar-aligned/types/metrics';
import { fetchOpenAPI, fetchWebApi } from '../web-api';
import { openApiTestData } from './data/web-api';

jest.mock('../web-api');

const BASE_DOMAINS = [
  {
    actions: [
      mockAction(),
      mockAction({ key: 'memos', description: 'get normal memos' }),
      mockAction({
        key: 'deprecated',
        description: 'deprecated action',
        deprecatedSince: '2012-07-23',
      }),
    ],
    description: 'foo',
    internal: false,
    path: 'foo/bar',
    since: '1.0',
  },
  {
    actions: [mockAction({ key: 'ia', description: 'get internal memos', internal: true })],
    description: 'internal stuff',
    internal: false,
    path: 'internal/thing1',
    since: '1.3',
  },
  {
    path: 'api/project_badges',
    actions: [
      mockAction({
        key: 'measure',
        params: [
          {
            key: 'metric',
            description: 'Badge Metric key',
            required: true,
            internal: false,
            possibleValues: [
              MetricKey.bugs,
              MetricKey.software_quality_reliability_issues,
              MetricKey.code_smells,
              MetricKey.software_quality_maintainability_issues,
              MetricKey.vulnerabilities,
              MetricKey.software_quality_security_issues,
              MetricKey.sqale_rating,
              MetricKey.software_quality_maintainability_rating,
              MetricKey.security_rating,
              MetricKey.software_quality_security_rating,
              MetricKey.reliability_rating,
              MetricKey.software_quality_reliability_rating,
              MetricKey.coverage,
              MetricKey.duplicated_lines_density,
              MetricKey.alert_status,
              MetricKey.security_hotspots,
              MetricKey.ncloc,
            ],
          },
        ],
      }),
    ],
    description: 'Badges API',
    internal: false,
    since: '1.3',
  },
];

export default class WebApiServiceMock {
  openApiDocument: OpenAPIV3.Document;
  domains;

  constructor() {
    this.openApiDocument = cloneDeep(openApiTestData);
    this.domains = cloneDeep(BASE_DOMAINS);

    jest.mocked(fetchOpenAPI).mockImplementation(this.handleFetchOpenAPI);
    jest.mocked(fetchWebApi).mockImplementation(this.handleFetchWebAPI);
  }

  handleFetchOpenAPI: typeof fetchOpenAPI = () => {
    return Promise.resolve(this.openApiDocument);
  };

  handleFetchWebAPI = () => {
    return Promise.resolve(this.domains);
  };

  reset = () => {
    this.openApiDocument = cloneDeep(openApiTestData);
    this.domains = cloneDeep(BASE_DOMAINS);
  };
}
