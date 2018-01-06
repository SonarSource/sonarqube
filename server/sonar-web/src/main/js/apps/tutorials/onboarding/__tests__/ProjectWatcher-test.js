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
// @flow
import React from 'react';
import { shallow, mount } from 'enzyme';
import ProjectWatcher from '../ProjectWatcher';

jest.mock('../../../../api/ce', () => ({
  getTasksForComponent: () => Promise.resolve({ current: { status: 'SUCCESS' }, queue: [] })
}));

jest.useFakeTimers();

it('renders', () => {
  const wrapper = shallow(
    <ProjectWatcher onFinish={jest.fn()} onTimeout={jest.fn()} projectKey="foo" />
  );
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({ inQueue: true });
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({ status: 'SUCCESS' });
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({ status: 'FAILED' });
  expect(wrapper).toMatchSnapshot();
});

it('finishes', done => {
  // checking `expect(onFinish).toBeCalled();` is not working, because it's called asynchronously
  // instead let's finish the test as soon as `onFinish` callback is called
  const onFinish = jest.fn(done);
  mount(<ProjectWatcher onFinish={onFinish} onTimeout={jest.fn()} projectKey="foo" />);
  expect(onFinish).not.toBeCalled();
  jest.runTimersToTime(5000);
});

it('timeouts', () => {
  const onTimeout = jest.fn();
  mount(<ProjectWatcher onFinish={jest.fn()} onTimeout={onTimeout} projectKey="foo" />);
  expect(onTimeout).not.toBeCalled();
  jest.runTimersToTime(10 * 60 * 1000);
  expect(onTimeout).toBeCalled();
});
