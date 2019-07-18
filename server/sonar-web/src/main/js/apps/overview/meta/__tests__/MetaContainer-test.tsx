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
import {
  mockAppState,
  mockComponent,
  mockLoggedInUser,
  mockOrganization
} from '../../../../helpers/testMocks';
import { Meta } from '../MetaContainer';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  expect(metaQualityGateRendered(wrapper)).toBe(true);
});

it('should hide QG and QP links if the organization has a paid plan, and the user is not a member', () => {
  const wrapper = shallowRender({
    organization: mockOrganization({ key: 'other_key', subscription: 'PAID' })
  });
  expect(wrapper).toMatchSnapshot();
  expect(metaQualityGateRendered(wrapper)).toBe(false);
});

it('should show QG and QP links if the organization has a paid plan, and the user is a member', () => {
  const wrapper = shallowRender({
    organization: mockOrganization({ subscription: 'PAID' })
  });
  expect(wrapper).toMatchSnapshot();
  expect(metaQualityGateRendered(wrapper)).toBe(true);
});

function metaQualityGateRendered(wrapper: any) {
  return wrapper.find('#overview-meta-quality-gate').exists();
}

function shallowRender(props: Partial<Meta['props']> = {}) {
  return shallow(
    <Meta
      appState={mockAppState({ organizationsEnabled: true })}
      component={mockComponent()}
      currentUser={mockLoggedInUser()}
      metrics={{}}
      onComponentChange={jest.fn()}
      organization={mockOrganization()}
      userOrganizations={[mockOrganization()]}
      {...props}
    />
  );
}
