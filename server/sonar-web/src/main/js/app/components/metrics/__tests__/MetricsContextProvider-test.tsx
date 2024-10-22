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

import * as React from 'react';
import { byRole } from '~sonar-aligned/helpers/testSelector';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { getAllMetrics } from '../../../../api/metrics';
import { mockMetric } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { MetricsContext } from '../MetricsContext';
import MetricsContextProvider from '../MetricsContextProvider';

jest.mock('../../../../api/metrics', () => ({
  getAllMetrics: jest.fn().mockResolvedValue([]),
}));

it('should call metric', async () => {
  const metrics = [
    mockMetric({ key: MetricKey.alert_status, name: 'Alert Status' }),
    mockMetric({ key: MetricKey.code_smells, name: 'Code Smells' }),
  ];
  jest.mocked(getAllMetrics).mockResolvedValueOnce(metrics);
  renderMetricsContextProvider();

  expect(await byRole('listitem').findAll()).toHaveLength(2);
});

function renderMetricsContextProvider() {
  return renderComponent(
    <MetricsContextProvider>
      <Consumer />
    </MetricsContextProvider>,
  );
}

function Consumer() {
  const metrics = React.useContext(MetricsContext);
  return (
    <ul>
      {Object.keys(metrics).map((k) => (
        <li key={k}>{metrics[k].name}</li>
      ))}
    </ul>
  );
}
