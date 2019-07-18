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
import { mockOrganization, mockOrganizationWithAlm } from '../../../helpers/testMocks';
import MembersListHeader, { Props } from '../MembersListHeader';

it('should render without the total', () => {
  expect(shallowRender({ total: undefined })).toMatchSnapshot();
});

it('should render with the total', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render a help tooltip', () => {
  expect(
    shallowRender({ organization: mockOrganizationWithAlm({}, { membersSync: true }) }).find(
      'HelpTooltip'
    )
  ).toMatchSnapshot();
  expect(
    shallowRender({
      organization: mockOrganizationWithAlm(
        {},
        { key: 'bitbucket', membersSync: true, url: 'https://bitbucket.com/foo' }
      )
    }).find('HelpTooltip')
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(
    <MembersListHeader
      handleSearch={jest.fn()}
      organization={mockOrganization()}
      total={8}
      {...props}
    />
  );
}
