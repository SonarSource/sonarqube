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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockComponent, mockLoggedInUser } from '../../../../../helpers/testMocks';
import { TutorialProps } from '../../utils';
import ConfigureWithTravis from '../ConfigureWithTravis';

jest.mock('sonar-ui-common/helpers/storage', () => ({
  get: jest.fn().mockReturnValue(
    JSON.stringify({
      build: 'maven',
      hasStepAfterTravisYml: true,
      step: 3
    })
  ),
  save: jest.fn()
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should react to EditTravisYmlStep onContinue', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('EditTravisYmlStep').prop('open') as Boolean).toBeFalsy();

  (wrapper.find('EncryptYourTokenStep').prop('onContinue') as Function)();

  expect(wrapper.find('EditTravisYmlStep').prop('open') as Boolean).toBeTruthy();
});

it('should react to EditTravisYmlStep onOpen', () => {
  const wrapper = shallowRender();
  (wrapper.find('EncryptYourTokenStep').prop('onContinue') as Function)();
  expect(wrapper.find('EncryptYourTokenStep').prop('open') as Boolean).toBeFalsy();

  (wrapper.find('EncryptYourTokenStep').prop('onOpen') as Function)();

  expect(wrapper.find('EncryptYourTokenStep').prop('open') as Boolean).toBeTruthy();
});

function shallowRender(props: Partial<TutorialProps> = {}) {
  return shallow(
    <ConfigureWithTravis
      component={mockComponent()}
      currentUser={mockLoggedInUser()}
      onDone={jest.fn()}
      setToken={jest.fn()}
      token="token123"
      {...props}
    />
  );
}
