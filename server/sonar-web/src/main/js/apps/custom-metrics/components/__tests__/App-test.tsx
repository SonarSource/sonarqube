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
import App from '../App';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/metrics', () => ({
  getMetricDomains: () => Promise.resolve(['Coverage', 'Issues']),
  getMetricTypes: () => Promise.resolve(['INT', 'STRING']),
  getMetrics: () =>
    Promise.resolve({
      metrics: [{ id: '3', key: 'foo', name: 'Foo', type: 'INT' }],
      p: 1,
      ps: 1,
      total: 1
    }),
  deleteMetric: () => Promise.resolve(),
  updateMetric: () => Promise.resolve(),
  createMetric: () =>
    Promise.resolve({ id: '4', domain: 'Coverage', key: 'bar', name: 'Bar', type: 'INT' })
}));

it('should work', async () => {
  const wrapper = shallow<App>(<App />);
  wrapper.instance().mounted = true;
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  // create
  wrapper.find('Header').prop<Function>('onCreate')({
    domain: 'Coverage',
    key: 'bar',
    name: 'Bar',
    type: 'INT'
  });
  await waitAndUpdate(wrapper);
  expect(wrapper.state().metrics).toMatchSnapshot();
  expect(wrapper.state().paging!.total).toBe(2);

  // edit
  wrapper.find('List').prop<Function>('onEdit')({
    domain: undefined,
    id: '4',
    key: 'bar',
    name: 'Bar',
    type: 'STRING'
  });
  await waitAndUpdate(wrapper);
  expect(wrapper.state().metrics).toMatchSnapshot();
  expect(wrapper.state().paging!.total).toBe(2);

  // delete
  wrapper.find('List').prop<Function>('onDelete')('bar');
  await waitAndUpdate(wrapper);
  expect(wrapper.state().metrics).toMatchSnapshot();
  expect(wrapper.state().paging!.total).toBe(1);
});
