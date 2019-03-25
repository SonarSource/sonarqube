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
import * as React from 'react';
import { shallow } from 'enzyme';
import MeasureContent from '../MeasureContent';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { getComponentTree } from '../../../../api/components';
import { mockComponentMeasure, mockRouter } from '../../../../helpers/testMocks';

jest.mock('../../../../api/components', () => {
  const { mockComponentMeasure } = require.requireActual('../../../../helpers/testMocks');
  return {
    getComponentTree: jest.fn().mockResolvedValue({
      paging: { pageIndex: 1, pageSize: 500, total: 2 },
      baseComponent: mockComponentMeasure(),
      components: [mockComponentMeasure(true)],
      metrics: [
        {
          bestValue: '0',
          custom: false,
          description: 'Bugs',
          domain: 'Reliability',
          hidden: false,
          higherValuesAreBetter: false,
          key: 'bugs',
          name: 'Bugs',
          qualitative: true,
          type: 'INT'
        }
      ]
    })
  };
});

jest.mock('../../../../api/measures', () => ({
  getMeasures: jest.fn().mockResolvedValue([{ metric: 'bugs', value: '12', bestValue: false }])
}));

const METRICS = {
  bugs: { id: '1', key: 'bugs', type: 'INT', name: 'Bugs', domain: 'Reliability' }
};

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly for a project', async () => {
  const wrapper = shallowRender();
  expect(wrapper.type()).toBeNull();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly for a file', async () => {
  (getComponentTree as jest.Mock).mockResolvedValueOnce({
    paging: { pageIndex: 1, pageSize: 500, total: 0 },
    baseComponent: mockComponentMeasure(true),
    components: [],
    metrics: [METRICS.bugs]
  });
  const wrapper = shallowRender();
  expect(wrapper.type()).toBeNull();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<MeasureContent['props']> = {}) {
  return shallow(
    <MeasureContent
      metrics={METRICS}
      requestedMetric={{ direction: 1, key: 'bugs' }}
      rootComponent={mockComponentMeasure()}
      router={mockRouter()}
      updateQuery={jest.fn()}
      view="list"
      {...props}
    />
  );
}
