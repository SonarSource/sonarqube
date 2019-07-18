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
import { shallow } from 'enzyme';
import * as React from 'react';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
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
      paging: { pageIndex: 1, pageSize: 1, total: 1 }
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
  const wrapper = shallow<App>(<App component={{ key: 'foo' }} />);
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  // create
  wrapper.find('Header').prop<Function>('onCreate')({
    description: 'description',
    metricKey: 'metricKey',
    value: 'value'
  });
  await waitAndUpdate(wrapper);
  expect(wrapper.state().measures).toMatchSnapshot();
  expect(wrapper.state().paging!.total).toBe(2);

  // edit
  wrapper.find('List').prop<Function>('onEdit')({
    description: 'another',
    id: '2',
    value: 'other'
  });
  await waitAndUpdate(wrapper);
  expect(wrapper.state().measures).toMatchSnapshot();
  expect(wrapper.state().paging!.total).toBe(2);

  // delete
  wrapper.find('List').prop<Function>('onDelete')('2');
  await waitAndUpdate(wrapper);
  expect(wrapper.state().measures).toMatchSnapshot();
  expect(wrapper.state().paging!.total).toBe(1);
});
