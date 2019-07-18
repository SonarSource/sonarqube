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
import ApplicationLeakPeriodLegend from '../ApplicationLeakPeriodLegend';

jest.mock('../../../../api/application', () => ({
  getApplicationLeak: jest.fn(() =>
    Promise.resolve([
      { date: '2017-01-01T11:39:03+0100', project: 'foo', projectName: 'Foo' },
      { date: '2017-02-01T11:39:03+0100', project: 'bar', projectName: 'Bar' }
    ])
  )
}));

it('renders', async () => {
  const wrapper = shallow(
    <ApplicationLeakPeriodLegend
      component={{ key: 'foo', organization: 'bar', qualifier: 'APP' }}
    />
  );
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});
