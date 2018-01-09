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
/* eslint-disable import/order */
import * as React from 'react';
import { mount, shallow } from 'enzyme';
import Stacktrace from '../Stacktrace';
import { click } from '../../../../helpers/testUtils';

jest.mock('react-dom');

jest.mock('../../../../api/ce', () => ({
  getTask: jest.fn(() => Promise.resolve({ errorStacktrace: 'stacktrace' }))
}));

const getTask = require('../../../../api/ce').getTask as jest.Mock<any>;

const task = {
  componentName: 'foo',
  status: 'PENDING',
  id: '123',
  submittedAt: '2017-01-01',
  type: 'REPORT'
};

beforeEach(() => {
  getTask.mockClear();
});

it('renders', () => {
  const wrapper = shallow(<Stacktrace onClose={jest.fn()} task={task} />);
  wrapper.setState({ loading: false, stacktrace: 'stacktrace' });
  expect(wrapper).toMatchSnapshot();
});

it('closes', () => {
  const onClose = jest.fn();
  const wrapper = shallow(<Stacktrace onClose={onClose} task={task} />);
  click(wrapper.find('.js-modal-close'));
  expect(onClose).toBeCalled();
});

it('fetches scanner context on mount', async () => {
  const wrapper = mount(<Stacktrace onClose={jest.fn()} task={task} />);
  expect(wrapper.state()).toEqual({ loading: true });
  expect(getTask).toBeCalledWith('123', ['stacktrace']);
  await new Promise(setImmediate);
  expect(wrapper.state()).toEqual({ loading: false, stacktrace: 'stacktrace' });
});
