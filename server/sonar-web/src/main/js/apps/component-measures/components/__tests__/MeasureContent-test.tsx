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
import { getComponentTree } from '../../../../api/components';
import { mockComponentMeasure } from '../../../../helpers/mocks/component';
import { mockRouter } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import MeasureContent from '../MeasureContent';

jest.mock('../../../../helpers/scrolling', () => ({
  scrollToElement: jest.fn(),
}));

jest.mock('../../../../api/components', () => {
  const { mockComponentMeasure } = jest.requireActual('../../../../helpers/mocks/component');
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
          type: 'INT',
        },
      ],
    }),
  };
});

jest.mock('../../../../api/measures', () => ({
  getMeasures: jest.fn().mockResolvedValue([{ metric: 'bugs', value: '12', bestValue: false }]),
}));

const METRICS = {
  bugs: { id: '1', key: 'bugs', type: 'INT', name: 'Bugs', domain: 'Reliability' },
};

const WINDOW_HEIGHT = 800;
const originalHeight = window.innerHeight;

beforeEach(() => {
  jest.clearAllMocks();
});

beforeAll(() => {
  Object.defineProperty(window, 'innerHeight', {
    writable: true,
    configurable: true,
    value: WINDOW_HEIGHT,
  });
});

afterAll(() => {
  Object.defineProperty(window, 'innerHeight', {
    writable: true,
    configurable: true,
    value: originalHeight,
  });
});

it('should render correctly for a project', async () => {
  const wrapper = shallowRender();
  expect(wrapper.type()).toBeNull();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly when asc prop is defined', async () => {
  const wrapper = shallowRender({ asc: true });
  expect(wrapper.type()).toBeNull();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly when view prop is tree', async () => {
  const wrapper = shallowRender({ view: 'tree' });
  expect(wrapper.type()).toBeNull();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly for a file', async () => {
  (getComponentTree as jest.Mock).mockResolvedValueOnce({
    paging: { pageIndex: 1, pageSize: 500, total: 0 },
    baseComponent: mockComponentMeasure(true),
    components: [],
    metrics: [METRICS.bugs],
  });
  const wrapper = shallowRender();
  expect(wrapper.type()).toBeNull();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should test fetchMoreComponents to work correctly', async () => {
  (getComponentTree as jest.Mock).mockResolvedValueOnce({
    paging: { pageIndex: 12, pageSize: 500, total: 0 },
    baseComponent: mockComponentMeasure(false),
    components: [],
    metrics: [METRICS.bugs],
  });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.instance().fetchMoreComponents();
  expect(wrapper).toMatchSnapshot();
});

it('should test getComponentRequestParams response for different arguments', () => {
  const wrapper = shallowRender({ asc: false });
  const metric = {
    direction: -1,
    key: 'new_reliability_rating',
  };
  const reqParamsList = {
    metricKeys: ['new_reliability_rating'],
    opts: {
      additionalFields: 'metrics',
      asc: true,
      metricPeriodSort: 1,
      metricSort: 'new_reliability_rating',
      metricSortFilter: 'withMeasuresOnly',
      ps: 500,
      s: 'metricPeriod',
    },
    strategy: 'leaves',
  };
  expect(wrapper.instance().getComponentRequestParams('list', metric, { asc: true })).toEqual(
    reqParamsList
  );
  // when options.asc is not passed the opts.asc will take the default value
  reqParamsList.opts.asc = false;
  expect(wrapper.instance().getComponentRequestParams('list', metric, {})).toEqual(reqParamsList);

  const reqParamsTreeMap = {
    metricKeys: ['new_reliability_rating', 'new_lines'],
    opts: {
      additionalFields: 'metrics',
      asc: true,
      metricPeriodSort: 1,
      metricSort: 'new_lines',
      metricSortFilter: 'withMeasuresOnly',
      ps: 500,
      s: 'metricPeriod',
    },
    strategy: 'children',
  };
  expect(wrapper.instance().getComponentRequestParams('treemap', metric, { asc: true })).toEqual(
    reqParamsTreeMap
  );
  // when options.asc is not passed the opts.asc will take the default value
  reqParamsTreeMap.opts.asc = false;
  expect(wrapper.instance().getComponentRequestParams('treemap', metric, {})).toEqual(
    reqParamsTreeMap
  );

  const reqParamsTree = {
    metricKeys: ['new_reliability_rating'],
    opts: {
      additionalFields: 'metrics',
      asc: false,
      ps: 500,
      s: 'qualifier,name',
    },
    strategy: 'children',
  };
  expect(wrapper.instance().getComponentRequestParams('tree', metric, { asc: false })).toEqual(
    reqParamsTree
  );
  // when options.asc is not passed the opts.asc will take the default value
  reqParamsTree.opts.asc = true;
  expect(wrapper.instance().getComponentRequestParams('tree', metric, {})).toEqual(reqParamsTree);
});

function shallowRender(props: Partial<MeasureContent['props']> = {}) {
  return shallow<MeasureContent>(
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
