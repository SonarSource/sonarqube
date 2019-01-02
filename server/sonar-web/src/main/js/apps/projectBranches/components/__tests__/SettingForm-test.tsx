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
jest.mock('../../../../api/settings', () => ({
  setSimpleSettingValue: jest.fn(() => Promise.resolve()),
  resetSettingValue: jest.fn(() => Promise.resolve())
}));

import * as React from 'react';
import { shallow } from 'enzyme';
import SettingForm from '../SettingForm';
import { change, submit, click } from '../../../../helpers/testUtils';

const setSimpleSettingValue = require('../../../../api/settings')
  .setSimpleSettingValue as jest.Mock<any>;

const resetSettingValue = require('../../../../api/settings').resetSettingValue as jest.Mock<any>;

beforeEach(() => {
  setSimpleSettingValue.mockClear();
  resetSettingValue.mockClear();
});

it('changes value', async () => {
  const onChange = jest.fn();
  const wrapper = shallow(
    <SettingForm
      onChange={onChange}
      onClose={jest.fn()}
      project="project"
      setting={{ inherited: true, key: 'foo', value: 'release-.*' }}
    />
  );
  expect(wrapper).toMatchSnapshot();

  change(wrapper.find('input'), 'branch-.*');
  submit(wrapper.find('form'));
  expect(setSimpleSettingValue).toBeCalledWith({
    branch: undefined,
    component: 'project',
    key: 'foo',
    value: 'branch-.*'
  });

  await new Promise(setImmediate);
  expect(onChange).toBeCalled();
});

it('resets value', async () => {
  const onChange = jest.fn();
  const wrapper = shallow(
    <SettingForm
      onChange={onChange}
      onClose={jest.fn()}
      project="project"
      setting={{ inherited: false, key: 'foo', parentValue: 'branch-.*', value: 'release-.*' }}
    />
  );
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('Button'));
  expect(resetSettingValue).toBeCalledWith({
    keys: 'foo',
    component: 'project',
    branch: undefined
  });

  await new Promise(setImmediate);
  expect(onChange).toBeCalled();
});
