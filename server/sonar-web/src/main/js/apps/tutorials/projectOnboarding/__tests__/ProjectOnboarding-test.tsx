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
import * as React from 'react';
import { shallow } from 'enzyme';
import { ProjectOnboarding } from '../ProjectOnboarding';
import { click, doAsync } from '../../../../helpers/testUtils';
import { getInstance, isSonarCloud } from '../../../../helpers/system';

jest.mock('../../../../api/users', () => ({
  skipOnboarding: () => Promise.resolve()
}));

jest.mock('../../../../helpers/system', () => ({
  getInstance: jest.fn(),
  isSonarCloud: jest.fn()
}));

const currentUser = { login: 'admin', isLoggedIn: true };

it('guides for on-premise', () => {
  (getInstance as jest.Mock<any>).mockImplementation(() => 'SonarQube');
  (isSonarCloud as jest.Mock<any>).mockImplementation(() => false);
  const wrapper = shallow(
    <ProjectOnboarding
      currentUser={currentUser}
      onFinish={jest.fn()}
      organizationsEnabled={false}
    />
  );
  expect(wrapper).toMatchSnapshot();

  (wrapper.instance() as ProjectOnboarding).handleTokenDone('abcd1234');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('guides for sonarcloud', () => {
  (getInstance as jest.Mock<any>).mockImplementation(() => 'SonarCloud');
  (isSonarCloud as jest.Mock<any>).mockImplementation(() => true);
  const wrapper = shallow(
    <ProjectOnboarding currentUser={currentUser} onFinish={jest.fn()} organizationsEnabled={true} />
  );
  expect(wrapper).toMatchSnapshot();

  (wrapper.instance() as ProjectOnboarding).handleOrganizationDone('my-org');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  (wrapper.instance() as ProjectOnboarding).handleTokenDone('abcd1234');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('finishes', () => {
  (getInstance as jest.Mock<any>).mockImplementation(() => 'SonarQube');
  (isSonarCloud as jest.Mock<any>).mockImplementation(() => false);
  const onFinish = jest.fn();
  const wrapper = shallow(
    <ProjectOnboarding currentUser={currentUser} onFinish={onFinish} organizationsEnabled={false} />
  );
  click(wrapper.find('ResetButtonLink'));
  return doAsync(() => {
    expect(onFinish).toBeCalled();
  });
});
