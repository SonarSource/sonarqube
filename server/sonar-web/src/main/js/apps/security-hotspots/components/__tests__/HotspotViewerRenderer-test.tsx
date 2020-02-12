/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import { mockHotspot } from '../../../../helpers/mocks/security-hotspots';
import { mockComponent, mockCurrentUser, mockUser } from '../../../../helpers/testMocks';
import { HotspotViewerRenderer, HotspotViewerRendererProps } from '../HotspotViewerRenderer';

jest.mock('../../../../helpers/users', () => ({ isLoggedIn: jest.fn(() => true) }));

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  expect(shallowRender({ hotspot: undefined })).toMatchSnapshot('no hotspot');
  expect(shallowRender({ hotspot: mockHotspot({ assignee: undefined }) })).toMatchSnapshot(
    'unassigned'
  );
  expect(
    shallowRender({ hotspot: mockHotspot({ assigneeUser: mockUser({ active: false }) }) })
  ).toMatchSnapshot('deleted assignee');
  expect(
    shallowRender({
      hotspot: mockHotspot({
        assigneeUser: mockUser({ name: undefined, login: 'assignee_login' })
      })
    })
  ).toMatchSnapshot('assignee without name');
  expect(shallowRender()).toMatchSnapshot('anonymous user');
});

function shallowRender(props?: Partial<HotspotViewerRendererProps>) {
  return shallow(
    <HotspotViewerRenderer
      branchLike={mockBranch()}
      component={mockComponent()}
      commentTextRef={React.createRef()}
      commentVisible={false}
      currentUser={mockCurrentUser()}
      hotspot={mockHotspot()}
      loading={false}
      onCloseComment={jest.fn()}
      onOpenComment={jest.fn()}
      onUpdateHotspot={jest.fn()}
      parentScrollRef={React.createRef()}
      securityCategories={{ 'sql-injection': { title: 'SQL injection' } }}
      {...props}
    />
  );
}
