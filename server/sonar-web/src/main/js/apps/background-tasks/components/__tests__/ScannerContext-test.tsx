/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
jest.mock('../../../../api/ce', () => ({
  getTask: jest.fn()
}));

import * as React from 'react';
import { mount, shallow } from 'enzyme';
import ScannerContext from '../ScannerContext';
import { click, doAsync } from '../../../../helpers/testUtils';

const getTask = require('../../../../api/ce').getTask as jest.Mock<any>;

const task = {
  componentName: 'foo',
  status: 'PENDING',
  id: '123',
  submittedAt: '2017-01-01',
  type: 'REPORT'
};

it('renders', () => {
  const wrapper = shallow(<ScannerContext onClose={jest.fn()} task={task} />);
  wrapper.setState({ scannerContext: 'context' });
  expect(wrapper).toMatchSnapshot();
});

it('closes', () => {
  const onClose = jest.fn();
  const wrapper = shallow(<ScannerContext onClose={onClose} task={task} />);
  click(wrapper.find('.js-modal-close'));
  expect(onClose).toBeCalled();
});

it('fetches scanner context on mount', () => {
  getTask.mockImplementation(() => Promise.resolve({ scannerContext: 'context' }));
  const wrapper = mount(<ScannerContext onClose={jest.fn()} task={task} />);
  expect(wrapper.state()).toEqual({});
  expect(getTask).toBeCalledWith('123', ['scannerContext']);
  return doAsync().then(() => {
    expect(wrapper.state()).toEqual({ scannerContext: 'context' });
  });
});
