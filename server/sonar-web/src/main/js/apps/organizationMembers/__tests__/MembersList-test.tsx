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
import MembersList from '../MembersList';
import { mockLoggedInUser, mockOrganization } from '../../../helpers/testMocks';

const members = [
  { login: 'admin', name: 'Admin Istrator', avatar: '', groupCount: 3 },
  { login: 'john', name: 'John Doe', avatar: '7daf6c79d4802916d83f6266e24850af', groupCount: 1 }
];

it('should render a list of members of an organization', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render "no results"', () => {
  expect(shallowRender({ members: [] })).toMatchSnapshot();
});

function shallowRender(props: Partial<MembersList['props']> = {}) {
  return shallow(
    <MembersList
      currentUser={mockLoggedInUser({ login: 'admin' })}
      members={members}
      organization={mockOrganization()}
      organizationGroups={[]}
      removeMember={jest.fn()}
      updateMemberGroups={jest.fn()}
      {...props}
    />
  );
}
