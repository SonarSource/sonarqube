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
import { Onboarding } from '../Onboarding';
import { click } from '../../../helpers/testUtils';

it('renders correctly', () => {
  expect(
    shallow(
      <Onboarding
        currentUser={{ isLoggedIn: true }}
        onFinish={jest.fn()}
        onOpenOrganizationOnboarding={jest.fn()}
        onOpenProjectOnboarding={jest.fn()}
        onOpenTeamOnboarding={jest.fn()}
      />
    )
  ).toMatchSnapshot();
});

it('should correctly open the different tutorials', () => {
  const onFinish = jest.fn();
  const onOpenOrganizationOnboarding = jest.fn();
  const onOpenProjectOnboarding = jest.fn();
  const onOpenTeamOnboarding = jest.fn();
  const wrapper = shallow(
    <Onboarding
      currentUser={{ isLoggedIn: true }}
      onFinish={onFinish}
      onOpenOrganizationOnboarding={onOpenOrganizationOnboarding}
      onOpenProjectOnboarding={onOpenProjectOnboarding}
      onOpenTeamOnboarding={onOpenTeamOnboarding}
    />
  );

  click(wrapper.find('ResetButtonLink'));
  expect(onFinish).toHaveBeenCalled();

  wrapper.find('Button').forEach(button => click(button));
  expect(onOpenOrganizationOnboarding).toHaveBeenCalled();
  expect(onOpenProjectOnboarding).toHaveBeenCalled();
  expect(onOpenTeamOnboarding).toHaveBeenCalled();
});
