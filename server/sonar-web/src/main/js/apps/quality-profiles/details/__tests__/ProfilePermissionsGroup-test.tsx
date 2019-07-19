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
jest.mock('../../../../api/quality-profiles', () => ({
  removeGroup: jest.fn(() => Promise.resolve())
}));

import { shallow } from 'enzyme';
import * as React from 'react';
import { click } from 'sonar-ui-common/helpers/testUtils';
import ProfilePermissionsGroup from '../ProfilePermissionsGroup';

const removeGroup = require('../../../../api/quality-profiles').removeGroup as jest.Mock<any>;

const profile = { language: 'js', name: 'Sonar way' };
const group = { name: 'lambda' };

beforeEach(() => {
  removeGroup.mockClear();
});

it('renders', () => {
  expect(
    shallow(<ProfilePermissionsGroup group={group} onDelete={jest.fn()} profile={profile} />)
  ).toMatchSnapshot();
});

it('removes user', async () => {
  const onDelete = jest.fn();
  const wrapper = shallow(
    <ProfilePermissionsGroup
      group={group}
      onDelete={onDelete}
      organization="org"
      profile={profile}
    />
  );
  (wrapper.instance() as ProfilePermissionsGroup).mounted = true;
  expect(wrapper.find('SimpleModal').exists()).toBeFalsy();

  click(wrapper.find('DeleteButton'));
  expect(wrapper.find('SimpleModal').exists()).toBeTruthy();

  wrapper.find('SimpleModal').prop<Function>('onSubmit')();
  expect(removeGroup).toBeCalledWith({
    group: 'lambda',
    language: 'js',
    organization: 'org',
    qualityProfile: 'Sonar way'
  });

  await new Promise(setImmediate);
  expect(onDelete).toBeCalledWith(group);
});
