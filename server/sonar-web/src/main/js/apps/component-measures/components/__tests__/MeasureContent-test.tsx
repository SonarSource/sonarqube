/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { scrollToElement } from '../../../../helpers/scrolling';
import { mockRouter } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import MeasureContent from '../MeasureContent';

jest.mock('../../../../helpers/scrolling', () => ({
  scrollToElement: jest.fn()
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

const WINDOW_HEIGHT = 800;
const originalHeight = window.innerHeight;

beforeEach(() => {
  jest.clearAllMocks();
});

beforeAll(() => {
  Object.defineProperty(window, 'innerHeight', {
    writable: true,
    configurable: true,
    value: WINDOW_HEIGHT
  });
});

afterAll(() => {
  Object.defineProperty(window, 'innerHeight', {
    writable: true,
    configurable: true,
    value: originalHeight
  });
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

it('should correctly handle scrolling', () => {
  const element = {} as Element;
  const wrapper = shallowRender();
  wrapper.instance().handleScroll(element);
  expect(scrollToElement).toBeCalledWith(element, {
    topOffset: 300,
    bottomOffset: 400,
    smooth: true
  });
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
