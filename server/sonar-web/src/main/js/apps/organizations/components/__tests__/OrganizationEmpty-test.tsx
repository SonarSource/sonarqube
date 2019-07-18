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
import {
  mockOrganization,
  mockOrganizationWithAlm,
  mockRouter
} from '../../../../helpers/testMocks';
import { OrganizationEmpty } from '../OrganizationEmpty';

const organization: T.Organization = mockOrganization();

it('should render', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should create new project', () => {
  const openProjectOnboarding = jest.fn();
  const wrapper = shallowRender({ openProjectOnboarding });

  click(wrapper.find('Button').first());
  expect(openProjectOnboarding).toBeCalledWith({ key: 'foo', name: 'Foo' });
});

it('should add members', () => {
  const push = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ push }) });
  click(wrapper.find('Button').last());
  expect(push).toBeCalledWith('/organizations/foo/members');
});

it('should hide add members button when member sync activated', () => {
  expect(
    shallowRender({ organization: mockOrganizationWithAlm({}, { membersSync: true }) })
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<OrganizationEmpty['props']> = {}) {
  return shallow(
    <OrganizationEmpty
      openProjectOnboarding={jest.fn()}
      organization={organization}
      router={mockRouter()}
      {...props}
    />
  );
}
