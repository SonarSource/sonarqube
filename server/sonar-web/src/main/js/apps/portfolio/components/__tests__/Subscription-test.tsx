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
/* eslint-disable import/first */
jest.mock('../../../../api/report', () => {
  const report = require.requireActual('../../../../api/report');
  report.subscribe = jest.fn(() => Promise.resolve());
  report.unsubscribe = jest.fn(() => Promise.resolve());
  return report;
});

import { mount, shallow } from 'enzyme';
import * as React from 'react';
import { click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { ReportStatus } from '../../../../api/report';
import { Subscription } from '../Subscription';

const subscribe = require('../../../../api/report').subscribe as jest.Mock<any>;
const unsubscribe = require('../../../../api/report').unsubscribe as jest.Mock<any>;

beforeEach(() => {
  subscribe.mockClear();
  unsubscribe.mockClear();
});

it('renders when subscribed', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('renders when not subscribed', () => {
  expect(shallowRender({}, { subscribed: false })).toMatchSnapshot();
});

it('renders when no email', () => {
  expect(shallowRender({ currentUser: { isLoggedIn: false } })).toMatchSnapshot();
});

it('changes subscription', async () => {
  const status = {
    canDownload: true,
    canSubscribe: true,
    componentFrequency: 'montly',
    globalFrequency: 'weekly',
    subscribed: true
  };

  const currentUser = { isLoggedIn: true, email: 'foo@example.com' };

  const wrapper = mount(
    <Subscription
      component="foo"
      currentUser={currentUser}
      onSubscribe={jest.fn()}
      status={status}
    />
  );

  click(wrapper.find('a'));
  expect(unsubscribe).toBeCalledWith('foo');

  wrapper.setProps({ status: { ...status, subscribed: false } });
  await waitAndUpdate(wrapper);

  click(wrapper.find('a'));
  expect(subscribe).toBeCalledWith('foo');
});

function shallowRender(
  props: Partial<Subscription['props']> = {},
  statusOverrides: Partial<ReportStatus> = {}
) {
  const status = {
    canDownload: true,
    canSubscribe: true,
    componentFrequency: 'montly',
    globalFrequency: 'weekly',
    subscribed: true,
    ...statusOverrides
  };

  const currentUser = { isLoggedIn: true, email: 'foo@example.com' };

  return shallow<Subscription>(
    <Subscription
      component="foo"
      currentUser={currentUser}
      onSubscribe={jest.fn()}
      status={status}
      {...props}
    />
  );
}
