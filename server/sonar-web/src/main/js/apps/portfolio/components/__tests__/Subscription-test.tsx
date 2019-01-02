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
  report.subscribe = jest.fn(() => Promise.resolve());
  report.unsubscribe = jest.fn(() => Promise.resolve());
  return report;
});

import * as React from 'react';
import { mount, shallow } from 'enzyme';
import Subscription from '../Subscription';
import { click, waitAndUpdate } from '../../../../helpers/testUtils';

const subscribe = require('../../../../api/report').subscribe as jest.Mock<any>;
const unsubscribe = require('../../../../api/report').unsubscribe as jest.Mock<any>;

const status = {
  canDownload: true,
  canSubscribe: true,
  componentFrequency: 'montly',
  globalFrequency: 'weekly',
  subscribed: true
};

const currentUser = { isLoggedIn: true, email: 'foo@example.com' };

beforeEach(() => {
  subscribe.mockClear();
  unsubscribe.mockClear();
});

it('renders when subscribed', () => {
  expect(
    shallow(<Subscription component="foo" currentUser={currentUser} status={status} />)
  ).toMatchSnapshot();
});

it('renders when not subscribed', () => {
  expect(
    shallow(
      <Subscription
        component="foo"
        currentUser={currentUser}
        status={{ ...status, subscribed: false }}
      />
    )
  ).toMatchSnapshot();
});

it('renders when no email', () => {
  expect(
    shallow(<Subscription component="foo" currentUser={{ isLoggedIn: false }} status={status} />)
  ).toMatchSnapshot();
});

it('changes subscription', async () => {
  const wrapper = mount(<Subscription component="foo" currentUser={currentUser} status={status} />);
  click(wrapper.find('button'));
  expect(unsubscribe).toBeCalledWith('foo');

  await waitAndUpdate(wrapper);

  click(wrapper.find('button'));
  expect(subscribe).toBeCalledWith('foo');
});
