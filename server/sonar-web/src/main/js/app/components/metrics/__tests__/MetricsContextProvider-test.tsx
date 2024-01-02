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
import { shallow } from 'enzyme';
import * as React from 'react';
import { getAllMetrics } from '../../../../api/metrics';
import { mockMetric } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import MetricsContextProvider from '../MetricsContextProvider';

jest.mock('../../../../api/metrics', () => ({
  getAllMetrics: jest.fn().mockResolvedValue({}),
}));

it('should call metric', async () => {
  const metrics = { coverage: mockMetric() };
  (getAllMetrics as jest.Mock).mockResolvedValueOnce(Object.values(metrics));
  const wrapper = shallowRender();

  expect(getAllMetrics).toHaveBeenCalled();
  await waitAndUpdate(wrapper);
  expect(wrapper.state()).toEqual({ metrics });
});

function shallowRender() {
  return shallow<MetricsContextProvider>(
    <MetricsContextProvider>
      <div />
    </MetricsContextProvider>
  );
}
