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
/* eslint-disable import/first, import/order */
jest.mock('../../../../api/report', () => {
  const report = require.requireActual('../../../../api/report');
  report.getReportStatus = jest.fn(() => Promise.resolve({}));
  return report;
});

import * as React from 'react';
import { mount, shallow } from 'enzyme';
import Report from '../Report';

const getReportStatus = require('../../../../api/report').getReportStatus as jest.Mock<any>;

const component = { key: 'foo', name: 'Foo' };

it('renders', () => {
  const wrapper = shallow(<Report component={component} />);
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({
    loading: false,
    status: {
      canDownload: true,
      canSubscribe: true,
      componentFrequency: 'montly',
      globalFrequency: 'weekly',
      subscribed: true
    }
  });
  expect(wrapper).toMatchSnapshot();
});

it('fetches status', () => {
  getReportStatus.mockClear();
  mount(<Report component={component} />);
  expect(getReportStatus).toBeCalledWith('foo');
});
