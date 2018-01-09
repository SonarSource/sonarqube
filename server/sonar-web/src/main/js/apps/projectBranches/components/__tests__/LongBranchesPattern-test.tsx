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
import { shallow } from 'enzyme';
import LongBranchesPattern from '../LongBranchesPattern';

jest.mock('../../../../api/settings', () => ({
  getValues: jest.fn(() => Promise.resolve([]))
}));

const getValues = require('../../../../api/settings').getValues as jest.Mock<any>;

beforeEach(() => {
  getValues.mockClear();
});

it('renders', () => {
  const wrapper = shallow(<LongBranchesPattern project="project" />);
  wrapper.setState({ loading: false, setting: { value: 'release-.*' } });
  expect(wrapper).toMatchSnapshot();
});

it('opens form', () => {
  const wrapper = shallow(<LongBranchesPattern project="project" />);
  wrapper.setState({ loading: false, setting: { value: 'release-.*' } });

  wrapper.find('EditButton').prop<Function>('onClick')();
  wrapper.update();
  expect(wrapper.find('LongBranchesPatternForm').exists()).toBeTruthy();

  wrapper.find('LongBranchesPatternForm').prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper.find('LongBranchesPatternForm').exists()).toBeFalsy();
});

it('fetches setting value on mount', () => {
  shallow(<LongBranchesPattern project="project" />);
  expect(getValues).lastCalledWith('sonar.branch.longLivedBranches.regex', 'project');
});

it('fetches new setting value after change', () => {
  const wrapper = shallow(<LongBranchesPattern project="project" />);
  expect(getValues).toHaveBeenCalledTimes(1);

  (wrapper.instance() as LongBranchesPattern).handleChange();
  expect(getValues).toHaveBeenCalledTimes(2);
});
