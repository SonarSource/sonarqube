/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import React from 'react';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockHotspot } from '../../../../helpers/mocks/security-hotspots';
import { mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import { HotspotSnippetHeader, HotspotSnippetHeaderProps } from '../HotspotSnippetHeader';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('user not logged in');
  expect(shallowRender({ currentUser: mockLoggedInUser() })).toMatchSnapshot('user logged in');
  expect(
    shallowRender({
      currentUser: mockLoggedInUser(),
      component: mockComponent({ qualifier: ComponentQualifier.Application }),
    })
  ).toMatchSnapshot('user logged in with project Name');
});

function shallowRender(props: Partial<HotspotSnippetHeaderProps> = {}) {
  return shallow(
    <HotspotSnippetHeader
      hotspot={mockHotspot()}
      currentUser={mockCurrentUser()}
      component={mockComponent()}
      {...props}
    />
  );
}
