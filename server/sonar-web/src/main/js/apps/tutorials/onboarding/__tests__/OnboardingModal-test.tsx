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
import { click } from 'sonar-ui-common/helpers/testUtils';
import { mockOrganization } from '../../../../helpers/testMocks';
import { OnboardingModal, Props } from '../OnboardingModal';

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
    userOrganizations: [
      mockOrganization({ key: 'a', name: 'Arthur' }),
      mockOrganization({ key: 'b', name: 'Boston Co' }),
      mockOrganization({ key: 'd', name: 'Daniel Inc' })
    ]
  });

  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('OrganizationsShortList').prop('organizations')).toHaveLength(3);
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(
    <OnboardingModal
      onClose={jest.fn()}
      onOpenProjectOnboarding={jest.fn()}
      userOrganizations={[]}
      {...props}
    />
  );
}
