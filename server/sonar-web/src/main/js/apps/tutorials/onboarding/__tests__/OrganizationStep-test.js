/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { mount } from 'enzyme';
import OrganizationStep from '../OrganizationStep';
import { click, doAsync } from '../../../../helpers/testUtils';

jest.mock('../../../../api/organizations', () => ({
  getMyOrganizations: () => Promise.resolve(['user', 'another'])
}));

const currentUser = { isLoggedIn: true, login: 'user' };

it('works with personal organization', () => {
  const onContinue = jest.fn();
  const wrapper = mount(
    <OrganizationStep
      currentUser={currentUser}
      finished={false}
      onContinue={onContinue}
      onOpen={jest.fn()}
      open={true}
      stepNumber={1}
    />
  );
  click(wrapper.find('.js-continue'));
  expect(onContinue).toBeCalledWith('user');
});

it('works with existing organization', () => {
  const onContinue = jest.fn();
  const wrapper = mount(
    <OrganizationStep
      currentUser={currentUser}
      finished={false}
      onContinue={onContinue}
      onOpen={jest.fn()}
      open={true}
      stepNumber={1}
    />
  );
  return doAsync(() => {
    click(wrapper.find('.js-existing'));
    wrapper.find('Select').prop('onChange')({ value: 'another' });
    click(wrapper.find('.js-continue'));
    expect(onContinue).toBeCalledWith('another');
  });
});

it('works with new organization', () => {
  const onContinue = jest.fn();
  const wrapper = mount(
    <OrganizationStep
      currentUser={currentUser}
      finished={false}
      onContinue={onContinue}
      onOpen={jest.fn()}
      open={true}
      stepNumber={1}
    />
  );
  click(wrapper.find('.js-new'));
  wrapper.find('NewOrganizationForm').prop('onDone')('new');
  click(wrapper.find('.js-continue'));
  expect(onContinue).toBeCalledWith('new');
});
