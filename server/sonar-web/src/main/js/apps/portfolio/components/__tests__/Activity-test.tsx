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
import { mount, shallow } from 'enzyme';
import Activity from '../Activity';
import { getAllTimeMachineData } from '../../../../api/time-machine';
import { getProjectActivityGraph } from '../../../projectActivity/utils';

jest.mock('../../../projectActivity/utils', () => {
  const utils = require.requireActual('../../../projectActivity/utils');
  utils.getProjectActivityGraph = jest
    .fn()
    .mockReturnValue({ graph: 'custom', customGraphs: ['coverage'] });
  return utils;
});

jest.mock('../../../../api/time-machine', () => ({
  getAllTimeMachineData: jest.fn().mockResolvedValue({
    measures: [
      {
        metric: 'coverage',
        history: [
          { date: '2017-01-01T00:00:00.000Z', value: '73' },
          { date: '2017-01-02T00:00:00.000Z', value: '82' }
        ]
      }
    ]
  })
}));

beforeEach(() => {
  (getAllTimeMachineData as jest.Mock).mockClear();
  (getProjectActivityGraph as jest.Mock).mockClear();
});

it('renders', () => {
  const wrapper = shallow(<Activity component="foo" metrics={{}} />);
  wrapper.setState({
    history: {
      coverage: [
        { date: '2017-01-01T00:00:00.000Z', value: '73' },
        { date: '2017-01-02T00:00:00.000Z', value: '82' }
      ]
    },
    loading: false,
    metrics: [{ key: 'coverage' }]
  });
  expect(wrapper).toMatchSnapshot();
  expect(getProjectActivityGraph).toBeCalledWith('foo');
});

it('fetches history', () => {
  mount(<Activity component="foo" metrics={{}} />);
  expect(getAllTimeMachineData).toBeCalledWith({ component: 'foo', metrics: 'coverage' });
});
