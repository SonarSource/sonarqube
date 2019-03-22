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
import * as React from 'react';
import { shallow } from 'enzyme';
import { OnboardingModal, Props } from '../OnboardingModal';
import { click } from '../../../../helpers/testUtils';
import { mockLoggedInUser, mockOrganization } from '../../../../helpers/testMocks';

it('renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should open project create page', () => {
  const onClose = jest.fn();
  const onOpenProjectOnboarding = jest.fn();
  const wrapper = shallowRender({ onClose, onOpenProjectOnboarding });

  click(wrapper.find('ResetButtonLink'));
  expect(onClose).toHaveBeenCalled();

  wrapper.find('Button').forEach(button => click(button));
  expect(onOpenProjectOnboarding).toHaveBeenCalled();
});

it('should display organization list if any', () => {
  const wrapper = shallowRender({
    currentUser: mockLoggedInUser({ personalOrganization: 'personal' }),
    userOrganizations: [
      mockOrganization({ key: 'a', name: 'Arthur' }),
      mockOrganization({ key: 'd', name: 'Daniel Inc' }),
      mockOrganization({ key: 'personal', name: 'Personal' })
    ]
  });

  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('OrganizationsShortList').prop('organizations')).toHaveLength(2);
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(
    <OnboardingModal
      currentUser={mockLoggedInUser()}
      onClose={jest.fn()}
      onOpenProjectOnboarding={jest.fn()}
      userOrganizations={[]}
      {...props}
    />
  );
}
