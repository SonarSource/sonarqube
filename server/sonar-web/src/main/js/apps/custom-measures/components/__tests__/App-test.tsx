/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

jest.mock('../../../../api/measures', () => ({
  getCustomMeasures: () =>
    Promise.resolve({
      customMeasures: [
        {
          createdAt: '2017-01-01',
          description: 'my custom measure',
          id: '1',
          metric: { key: 'custom', name: 'custom-metric', type: 'STRING' },
          projectKey: 'foo',
          user: { active: true, login: 'user', name: 'user' },
          value: 'custom-value'
        }
      ],
      p: 1,
      ps: 1,
      total: 1
    }),
  createCustomMeasure: () =>
    Promise.resolve({
      createdAt: '2018-01-01',
      description: 'description',
      id: '2',
      metric: { key: 'metricKey', name: 'Metric Name', type: 'STRING' },
      projectKey: 'foo',
      user: { active: true, login: 'user', name: 'user' },
      value: 'value'
    }),
  updateCustomMeasure: () => Promise.resolve(),
  deleteCustomMeasure: () => Promise.resolve()
}));

it('should work', async () => {
  const wrapper = shallow(<App component={{ key: 'foo' }} />);
  (wrapper.instance() as App).mounted = true;
  expect(wrapper).toMatchSnapshot();

  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  // create
  wrapper.find('Header').prop<Function>('onCreate')({
    description: 'description',
    metricKey: 'metricKey',
    value: 'value'
  });
  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper.state().measures).toMatchSnapshot();
  expect(wrapper.state().paging.total).toBe(2);

  // edit
  wrapper.find('List').prop<Function>('onEdit')({
    description: 'another',
    id: '2',
    value: 'other'
  });
  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper.state().measures).toMatchSnapshot();
  expect(wrapper.state().paging.total).toBe(2);

  // delete
  wrapper.find('List').prop<Function>('onDelete')('2');
  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper.state().measures).toMatchSnapshot();
  expect(wrapper.state().paging.total).toBe(1);
});
